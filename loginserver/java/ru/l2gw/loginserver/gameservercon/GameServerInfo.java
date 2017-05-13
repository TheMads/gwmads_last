package ru.l2gw.loginserver.gameservercon;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.network.utils.AdvIP;
import ru.l2gw.loginserver.GameServerTable;
import ru.l2gw.loginserver.gameservercon.gspackets.ServerStatus;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @Author: Death
* @Date: 16/11/2007
* @Time: 12:09:53
*/
public class GameServerInfo
{
	private static Log log = LogFactory.getLog(GameServerInfo.class.getName());
	private static final byte[] _emptyInfo = {0, 0};
	// auth
	private int _id;
	private byte[] _hexId;
	private boolean _isAuthed;

	// status
	private AttGS _gst;
	private int _status;

	// network
	private String _internalHost;
	private String _externalHost;
	private int _port;

	// config
	private boolean _isPvp = true;
	private boolean _isTestServer;
	private boolean _isShowingClock;
	private boolean _isShowingBrackets;
	private int _maxPlayers;
	private GArray<AdvIP> _ips;
	private final FastMap<String, byte[]> _accountInfo;

	public GameServerInfo(int id, byte[] hexId, AttGS gameserver)
	{
		_id = id;
		_hexId = hexId;
		_gst = gameserver;
		_accountInfo = new FastMap<String, byte[]>().shared();
	}

	public GameServerInfo(int id, byte[] hexId)
	{
		this(id, hexId, null);
	}

	public void setId(int id)
	{
		_id = id;
	}

	public int getId()
	{
		return _id;
	}

	public byte[] getHexId()
	{
		return _hexId;
	}

	public void setAuthed(boolean isAuthed)
	{
		_isAuthed = isAuthed;
	}

	public boolean isAuthed()
	{
		return _isAuthed;
	}

	public void setGameServer(AttGS gameserver)
	{
		_gst = gameserver;
	}

	public AttGS getGameServer()
	{
		return _gst;
	}

	public void setStatus(int status)
	{
		_status = status;
	}

	public int getStatus()
	{
		return _status;
	}

	public int getCurrentPlayerCount()
	{
		if(_gst == null)
			return 0;
		return _gst.getPlayerCount();
	}

	public int getFakePlayerCount()
	{
		if(_gst == null)
			return 0;
		return _gst.getFakePlayerCount();
	}

	public String getInternalIp()
	{
		if(_internalHost == null)
			return null;

		if(!_internalHost.equals("*"))
			try
			{
				return InetAddress.getByName(_internalHost).getHostAddress();
			}
			catch(UnknownHostException e)
			{
				e.printStackTrace();
			}

		return "*";
	}

	public String getInternalHost()
	{
		return _internalHost;
	}

	public void setInternalHost(String internalHost)
	{
		_internalHost = internalHost;
	}

	public String getExternalIp()
	{
		if(_externalHost == null)
			return null;

		if(!_externalHost.equals("*"))
			try
			{
				return InetAddress.getByName(_externalHost).getHostAddress();
			}
			catch(UnknownHostException e)
			{
				e.printStackTrace();
			}

		return "*";
	}

	public void setExternalHost(String externalHost)
	{
		_externalHost = externalHost;
	}

	public String getExternalHost()
	{
		return _externalHost;
	}

	public int getPort()
	{
		return _port;
	}

	public void setPort(int port)
	{
		_port = port;
	}

	public void setMaxPlayers(int maxPlayers)
	{
		_maxPlayers = maxPlayers;
	}

	public int getMaxPlayers()
	{
		//if(Config.FAKE_PLAYERS_FACTOR > 0)
		//	return (int)(L2ObjectsStorage.getAllPlayersCount() * Config.FAKE_PLAYERS_FACTOR * Config.FAKE_PLAYERS_FACTOR_HOUR[Calendar.getInstance().get(Calendar.HOUR_OF_DAY)]);

		return _maxPlayers;
	}

	public boolean isPvp()
	{
		return _isPvp;
	}

	public void setTestServer(boolean val)
	{
		_isTestServer = val;
	}

	public boolean isTestServer()
	{
		return _isTestServer;
	}

	public void setShowingClock(boolean clock)
	{
		_isShowingClock = clock;
	}

	public boolean isShowingClock()
	{
		return _isShowingClock;
	}

	public void setShowingBrackets(boolean val)
	{
		_isShowingBrackets = val;
	}

	public boolean isShowingBrackets()
	{
		return _isShowingBrackets;
	}

	public void setAdvIP(GArray<AdvIP> val)
	{
		_ips = val;
	}

	public GArray<AdvIP> getAdvIP()
	{
		return _ips;
	}

	public void setDown()
	{
		setAuthed(false);
		setPort(0);
		setGameServer(null);
		_status = ServerStatus.STATUS_DOWN;
	}

	@Override
	public String toString()
	{
		TextBuilder tb = TextBuilder.newInstance();

		tb.append("GameServer: ");
		if(_gst != null)
		{
			tb.append(_gst.getName());
			tb.append(" id:");
			tb.append(_id);
			tb.append(" hex:");
			tb.append(_hexId);
			tb.append(" ip:");
			tb.append(_gst.getConnectionIpAddress());
			tb.append(":");
			tb.append(_port);
			tb.append(" status: ");
			tb.append(ServerStatus.statusString[_status]);
		}
		else
		{
			tb.append(GameServerTable.getInstance().getServerNames().get(_id));
			tb.append(" id:");
			tb.append(_id);
			tb.append(" hex:");
			tb.append(_hexId);
			tb.append(" status: ");
			tb.append(ServerStatus.statusString[_status]);
		}

		String ret = tb.toString();
		TextBuilder.recycle(tb);
		return ret;
	}

	public void setGameHosts(String gameExternalHost, String gameInternalHost, GArray<AdvIP> ips)
	{
		setExternalHost(gameExternalHost);
		setInternalHost(gameInternalHost);
		setAdvIP(ips);

		log.info("Updated Gameserver " + GameServerTable.getInstance().getServerNameById(getId()) + " IP's:");
		log.info("InternalIP: " + getInternalHost());
		log.info("ExternalIP: " + getExternalHost());
	}

	public void updateAccountInfo(String account, byte chars, byte delChars)
	{
		_accountInfo.put(account, new byte[]{chars, delChars});
	}

	public byte[] getAccountInfo(String account)
	{
		if(_accountInfo.containsKey(account))
			return _accountInfo.get(account);

		return _emptyInfo;
	}
}
