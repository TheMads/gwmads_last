package ru.l2gw.gameserver.network;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.network.MMOClient;
import ru.l2gw.commons.network.MMOConnection;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.extensions.ccpGuard.ProtectInfo;
import ru.l2gw.extensions.ccpGuard.Protection;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.SessionKey;
import ru.l2gw.gameserver.loginservercon.gspackets.PlayerLogout;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.network.PacketFloodProtector.ActionType;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.serverpackets.NetPing;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Represents a client connected on Game Server
 */
public final class GameClient extends MMOClient<MMOConnection<GameClient>>
{
	protected static final Log _log = LogFactory.getLog("network");
	protected static final Log _logHWID = LogFactory.getLog("hwid");

	public GameCrypt _crypt;
	public ProtectInfo _prot_info = null;
	public GameClientState _state;
	private int _upTryes = 0;
	private long _upLastConnection = 0;
	private long _premiumExpire;
	private String allowdIps;
	public boolean charLoaded = false;
	private SecondAuthInfo secondAuthInfo;

	public static enum GameClientState
	{
		CONNECTED,
		AUTHED,
		IN_GAME,
		DISCONNECTED
	}

	private String _loginName;
	private int _accountId;
	private L2Player player;
	private SessionKey _sessionId;
	private final MMOConnection<GameClient> _connection;

	//private byte[] _filter;

	private int revision = 0;
	private boolean _gameGuardOk = false;

	//public boolean protect_used = false;
	public byte client_lang = -1;
	//public String HWID = "";

	private ArrayList<Integer> _charSlotMapping = new ArrayList<Integer>();

	// Flood Protect
	private Map<Integer, Long> _packets;

	protected ScheduledFuture<?> _netPingTask;
	private ScheduledFuture<?> _lobbyTimeout;

	// NetPing Data
	private short _seqSend = 1000;
	private short _rndSend = 0;
	private short _seqResive = 0;
	private short _rndResive = 0;
	private short _lostPackets = 0;
	private long _lastPingSendTime;
	private int _lastPing;
	
	public GameClient(MMOConnection<GameClient> con, boolean offline)
	{
		super(con);
		_state = GameClientState.CONNECTED;
		_sessionId = new SessionKey(-1, -1, -1, -1);
		if(offline)
		{
			_connection = null;
			_state = GameClientState.IN_GAME;
		}
		else
			_connection = con;
		_crypt = new GameCrypt();
		_prot_info = new ProtectInfo(this, getIpAddr(), offline);

		_packets = new FastMap<Integer, Long>();
		secondAuthInfo = new SecondAuthInfo(this);

		if(Config.PING_ENABLED && !offline)
			_netPingTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PingTask(), Config.PING_INTERVAL, Config.PING_INTERVAL);
	}

	public GameClient(MMOConnection<GameClient> con)
	{
		this(con, false);
	}

	public void disconnectOffline()
	{
		onDisconnection();
	}

	@Override
	protected void onDisconnection()
	{
		if(getLoginName() == null || getLoginName().equals("") || _state != GameClientState.IN_GAME && _state != GameClientState.AUTHED)
			return;

		Protection.doDisconection(this);

		if(Config.PING_ENABLED)
		{
			if(_netPingTask != null)
				_netPingTask.cancel(true);
			_netPingTask = null;
			_rndSend = 0;
			_seqSend = 1000;
		}
		
		try
		{
			//Возможноный фикс 2-х окон при оффлайн торговле
			//if(Config.PROTECT_ENABLE)
			//{
			//	if(player != null && player.isInOfflineMode())
			//	{
			//		AutoSaveManager.getInstance().removePlayer(player);
			//	}
			//}

			if(player != null && player.isInOfflineMode())
				return;

			LSConnection.getInstance().removeAccount(this);

			if(player != null && !player.isLogoutStarted()) // this should only happen on connection loss
			{
				L2Player tempPlayer = player;
				tempPlayer.setLogoutStarted(true);
				player.prepareToLogout(false);
				player = null;
				if(tempPlayer.getNetConnection() != null)
				{
					tempPlayer.getNetConnection().closeNow(false);
					tempPlayer.setNetConnection(null);
				}
				tempPlayer = null;
			}
			setConnection(null);
		}
		catch(Exception e1)
		{
			_log.warn("error while disconnecting client", e1);
		}
		finally
		{
			LSConnection.getInstance().sendPacket(new PlayerLogout(getLoginName()));
		}

		_state = GameClientState.DISCONNECTED;
		ThreadPoolManager.getInstance().onClientDisconnection(this);

	}

	public void deleteFromClan(L2Player player)
	{
		L2Clan clan = player.getClan();
		if(clan != null)
			clan.removeClanMember(player.getObjectId());
	}

	public static void deleteFromClan(int charId, int clanId)
	{
		if(clanId == 0)
			return;
		L2Clan clan = ClanTable.getInstance().getClan(clanId);
		if(clan != null)
			clan.removeClanMember(charId);
	}

	public void markRestoredChar(int charslot) throws Exception
	{
		//have to make sure active character must be nulled
		if(player != null)
		{
			player.saveCharToDisk();
			if(Config.DEBUG)
				_log.info("active Char saved");
			player = null;
		}

		int objid = getObjectIdForSlot(charslot);
		if(objid < 0)
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("data error on restore char:", e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void markToDeleteChar(int charslot) throws Exception
	{
		//have to make sure active character must be nulled
		if(player != null)
		{
			player.saveCharToDisk();
			if(Config.DEBUG)
				_log.info("active Char saved");
			player = null;
		}

		int objid = getObjectIdForSlot(charslot);
		if(objid < 0)
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE obj_id=?");
			statement.setLong(1, (int) (System.currentTimeMillis() / 1000));
			statement.setInt(2, objid);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("data error on update deletime char:", e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void deleteChar(int charslot) throws Exception
	{
		//have to make sure active character must be nulled
		if(player != null)
		{
			player.saveCharToDisk();
			if(Config.DEBUG)
				_log.info("active Char saved");
			player = null;
		}

		int objid = getObjectIdForSlot(charslot);
		if(objid == -1)
			return;

		deleteCharByObjId(objid);
	}

	public static void deleteCharByObjId(int objid)
	{
		if(objid < 0)
			return;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM characters WHERE obj_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_blocklist WHERE obj_Id=? or target_Id=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_botreports WHERE char_id=? or bot_id=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_variables WHERE obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE pets FROM pets, items WHERE pets.item_obj_id=items.object_id AND items.owner_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_summons WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_summons_effects WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM augmentations WHERE item_id IN (SELECT object_id FROM items WHERE owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_effects_save WHERE char_obj_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? or friend_id=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_contactlist WHERE char_id=? or contact_id=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM couples WHERE player1Id=? or player2Id=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM seven_signs WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_subclasses WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM olymp_nobles WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM olymp_nobles_prev WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM hero_history WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM hero_life_history WHERE hero_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM bbs_mail WHERE to_object_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM bbs_favorites WHERE object_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM bbs_favorites WHERE object_id=?");
			statement.setInt(1, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM character_mail WHERE dst_obj_id=? or src_obj_id=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = null;
		}
		catch(Exception e)
		{
			_log.warn("data error on delete char:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public synchronized L2Player loadCharFromDisk(int charslot)
	{
		if(charLoaded)
			return null;

		charLoaded = true;

		Integer objectId = getObjectIdForSlot(charslot);
		if(objectId == -1)
			return null;

		L2Object object = L2ObjectsStorage.findObject(objectId);
		if(object != null)
			if(object.isPlayer())
			{
				L2Player player = (L2Player) object;
				//_log.warn(player.toFullString() + " tried to make a clone.");
				if(!player.isInOfflineMode())
				{
					player.sendPacket(new SystemMessage(SystemMessage.ANOTHER_PERSON_HAS_LOGGED_IN_WITH_THE_SAME_ACCOUNT));
					LSConnection.getInstance().sendPacket(new PlayerLogout(getLoginName()));
					player.logout(false, false, true);
				}
				else
				{
					player.setOfflineMode(false);
					//player.logout(false, false, true);

					if(player.getNetConnection() != null)
					{
						player.getNetConnection().onDisconnection();
					}
					else
					{
						player.store();
					}
				}
			}

		L2Player player = L2Player.load(objectId, getHWID());

		if(player != null)
		{
			// preinit some values for each login
			player.setRunning(); // running is default
			player.standUp(); // standing is default

			player.updateStats();
			player.setOnlineStatus(true);
			setPlayer(player);

			switch (client_lang)
			{
				case 0:
					player.setVar("lang@", "en");
					break;
				case 1:
					player.setVar("lang@", "ru");
					break;
			}

			if(_prot_info.protect_used && ConfigProtect.PROTECT_GS_STORE_HWID)
				player.storeHWID(getHWID(), _logHWID);
		}
		else
			_log.warn("could not restore in slot:" + charslot);

		return player;
	}

	public int getObjectIdForSlot(int charslot)
	{
		if(charslot < 0 || charslot >= _charSlotMapping.size())
		{
			_log.warn(getLoginName() + " tried to modify Character in slot " + charslot + " but no characters exits at that slot.");
			return -1;
		}
		return _charSlotMapping.get(charslot);
	}

	@Override
	public MMOConnection<GameClient> getConnection()
	{
		return _connection;
	}

	public L2Player getPlayer()
	{
		return player;
	}

	/**
	 * @return Returns the sessionId.
	 */
	public SessionKey getSessionId()
	{
		return _sessionId;
	}

	public String getLoginName()
	{
		return _loginName;
	}

	public void setLoginName(String loginName)
	{
		_loginName = loginName;
	}

	public void setPlayer(L2Player player)
	{
		this.player = player;
		if(this.player != null)
		{
			// we store the connection in the player object so that external
			// events can directly send events to the players client
			// might be changed later to use a central event management and distribution system
			this.player.setNetConnection(this);

			// update world data
			//L2World.addObject(this.player);
		}
	}

	public void setSessionId(SessionKey sessionKey)
	{
		_sessionId = sessionKey;
	}

	public void setCharSelection(CharSelectInfoPackage[] chars)
	{
		_charSlotMapping.clear();

		for(CharSelectInfoPackage element : chars)
		{
			int objectId = element.getObjectId();
			_charSlotMapping.add(objectId);
		}
	}

	public void setCharSelection(int c)
	{
		_charSlotMapping.clear();
		_charSlotMapping.add(c);
	}

	/**
	 * @return Returns the revision.
	 */
	public int getRevision()
	{
		return revision;
	}

	/**
	 * @param revision The revision to set.
	 */
	public void setRevision(int revision)
	{
		this.revision = revision;
	}

	public void setGameGuardOk(boolean gameGuardOk)
	{
		_gameGuardOk = gameGuardOk;
	}

	public boolean isGameGuardOk()
	{
		return _gameGuardOk;
	}

	@Override
	public boolean encrypt(final ByteBuffer buf, final int size)
	{
		_crypt.encrypt(buf.array(), buf.position(), size);
		buf.position(buf.position() + size);
		return true;
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		_crypt.decrypt(buf.array(), buf.position(), size);
		return true;
	}

	public void sendPacket(L2GameServerPacket gsp)
	{
		if(getConnection() == null)
			return;
		getConnection().sendPacket(gsp);
	}

	public String getIpAddr()
	{
		try
		{
			return _connection.getSocket().getInetAddress().getHostAddress();
		}
		catch(NullPointerException e)
		{
			return "Disconnected";
		}
	}

	public byte[] enableCrypt()
	{
		byte[] key = BlowFishKeygen.getRandomKey();
		_crypt.setKey(key, _prot_info.protect_used);
		return key;
	}

	public long getPremiumExpire()
	{
		return _premiumExpire;
	}

	public void setPremiumExpire(long premiumExpire)
	{
		_premiumExpire = premiumExpire;
	}

	public GameClientState getState()
	{
		return _state;
	}

	public void setState(GameClientState state)
	{
		_state = state;
		if(_state == GameClientState.AUTHED)
		{
			if(_lobbyTimeout != null)
				_lobbyTimeout.cancel(true);
			_lobbyTimeout = ThreadPoolManager.getInstance().scheduleGeneral(new LobbyTimeout(), Config.LOBBY_TIMEOUT);
		}
		else if(_lobbyTimeout != null)
		{
			_lobbyTimeout.cancel(true);
			_lobbyTimeout = null;
		}
	}

	public void addUPTryes()
	{
		if(_upLastConnection != 0 && System.currentTimeMillis() - _upLastConnection > 1000)
			_upTryes = 0;
		_upTryes++;
		_upLastConnection = System.currentTimeMillis();
	}

	public int getUPTryes()
	{
		if(_upLastConnection != 0 && System.currentTimeMillis() - _upLastConnection > 1000)
		{
			_upTryes = 0;
			_upLastConnection = System.currentTimeMillis();
		}
		return _upTryes;
	}

	public ActionType checkPacket(int packetId)
	{
		PacketFloodProtector.PacketData pd = PacketFloodProtector.getInstance().getDataByPacketId(packetId);
		if(pd != null)
		{
			if(_packets.containsKey(packetId))
			{
				if(pd.getDelay() > System.currentTimeMillis() - _packets.get(packetId))
				{
					_packets.put(packetId, System.currentTimeMillis());
					return pd.getAction();
				}
				else
				{
					_packets.put(packetId, System.currentTimeMillis());
					return ActionType.none;
				}
			}
			else
			{
				_packets.put(packetId, System.currentTimeMillis());
				return ActionType.none;
			}
		}
		return ActionType.none;
	}

	public void pingReceived(short rnd, short seq)
	{
		_rndResive = rnd;
		_seqResive = seq;
		_lastPing = (int)(System.currentTimeMillis() - _lastPingSendTime);
	}

	public void stopPingTask()
	{
		if(_netPingTask != null)
		{
			_netPingTask.cancel(true);
			_netPingTask = null;
		}
	}

	class PingTask implements Runnable
	{
		public void run()
		{
			if(_state != GameClientState.IN_GAME)
				return;

			if(getPlayer() != null && getPlayer().isInOfflineMode())
			{
				_netPingTask.cancel(true);
				_netPingTask = null;
				return;
			}

			if(_rndSend != 0)
			{
				// chek resieved
				if(_rndSend != _rndResive || _seqSend != _seqResive)
				{
					if(getPlayer() != null)
					{
						if(_lostPackets >= Config.PING_MAX_LOST)
						{
							_log.warn("NetPing: " + GameClient.this + " No ping resieved or wrong packet. Expected " + _rndSend + "/" + _seqSend + " resived " + _rndResive + "/" + _seqResive);
							getPlayer().logout(false, true, false);
							return;
						}
						else
							_lostPackets++;
					}
					else
						return;
				}
				else
					_lostPackets = 0;
			}
			_rndSend = (short) Rnd.get(65535);
			_seqSend++;

			if(getPlayer() != null)
			{
				_lastPingSendTime = System.currentTimeMillis();
				getPlayer().sendPacket(new NetPing(_rndSend, _seqSend));
			}
		}
	}

	class LobbyTimeout implements Runnable
	{
		public void run()
		{
			if(_state == GameClientState.AUTHED)
				close(Msg.ServerClose);

			_lobbyTimeout = null;
		}
	}

	public int getPingTime()
	{
		return _lastPing;
	}

	public String getHWID()
	{
		return _prot_info.getHWID();
	}

	public void setAllowdIps(String ips)
	{
		allowdIps = ips;
	}

	public String getAllowdIps()
	{
		return allowdIps != null ? allowdIps : "";
	}

	public void setAccountId(int id)
	{
		_accountId = id;
	}

	public int getAccountId()
	{
		return _accountId;
	}

	@Override
	public String toString()
	{
		try
		{
			switch (getState())
			{
				case CONNECTED:
					return "[IP: " + getIpAddr() + "]";
				case AUTHED:
					return "[Account: " + getLoginName() + " - IP: " + getIpAddr() + "]";
				case IN_GAME:
					return "[Character: " + (getPlayer() == null ? "disconnected" : getPlayer().getName()) + " - Account: " + getLoginName() + " - IP: " + getIpAddr() + (getPlayer() == null ? "]" : " at " + getPlayer().getX() + "," + getPlayer().getY() + "," + getPlayer().getZ() + "]");
				case DISCONNECTED:
					return "[disconnected]";
				default:
					return "[unknown state]";
			}
		}
		catch(NullPointerException e)
		{
			return "[Character read failed due to disconnect]";
		}
	}

	public SecondAuthInfo getSecondAuthInfo()
	{
		return secondAuthInfo;
	}
}
