package ru.l2gw.extensions.ccpGuard.managers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.extensions.ccpGuard.ProtectInfo;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.network.GameClient;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class ProtectManager
{
	protected static Log _log = LogFactory.getLog("protect");
	private static ProtectManager _instance;
	private static ScheduledFuture<GGTask> _GGTask = null;
	private static long _lastL2ExtIpUpdate;
	private static byte[] _l2extIp;

	private class InfoSet
	{
		public String _playerName = "";

		public long _lastGGSendTime;
		public long _lastGGRecvTime;
		public int _attempts;
		public String _HWID = "";

		public InfoSet(final String name, final String HWID)
		{
			_playerName = name;
			_lastGGSendTime = System.currentTimeMillis();
			_lastGGRecvTime = _lastGGSendTime;
			_attempts = 0;
			_HWID = HWID;
		}
	}

	private static ConcurrentHashMap<String, InfoSet> _objects = new ConcurrentHashMap<String, InfoSet>();

	class GGTask implements Runnable
	{
		public void run()
		{
			long time = System.currentTimeMillis();
			for(final InfoSet object : _objects.values())
			{
				//Нужно ли отправить пакет на ГГ
				if(time - object._lastGGSendTime >= ConfigProtect.PROTECT_GG_SEND_INTERVAL)
				{
					try
					{
						L2ObjectsStorage.getPlayer(object._playerName).sendPacket(Msg.GameGuardQuery);
						object._lastGGSendTime = time;
						//ToDo сделать через перменную lock
						object._lastGGRecvTime = time + ConfigProtect.PROTECT_GG_RECV_INTERVAL + 1;
					}
					catch(final Exception e)
					{
						removePlayer(object._playerName);
					}
				}
				//ToDo через перменную lock, заблокировать до следующей проверки
				//Проверка пришёл ли ответ от клиента
				if(time - object._lastGGRecvTime >= ConfigProtect.PROTECT_GG_RECV_INTERVAL)
				{
					try
					{
						final L2Player player = L2ObjectsStorage.getPlayer(object._playerName);
						if(!player.getNetConnection().isGameGuardOk())
						{
							//если не пришёл увеличиваем кол-во
							if(object._attempts < 3)
							{
								object._attempts++;
							}
							else
							{
								if(player != null)
								{
									final GameClient client = player.getNetConnection();
									_log.info(client._prot_info + ": was kicked because GG packet not receive (3 attempts).");
								}
								player.logout(false, false, true);
							}
						}
						object._lastGGRecvTime = time;
					}
					catch(final Exception e)
					{
						removePlayer(object._playerName);
					}
				}
			}

			time = System.currentTimeMillis() - time;
			if(time > ConfigProtect.PROTECT_TASK_GG_INVERVAL)
				_log.info("ALERT! TASK_SAVE_INTERVAL is too small, time to save characters in Queue = " + time + ", Config=" + ConfigProtect.PROTECT_TASK_GG_INVERVAL);
		}
	}

	public void addPlayer(ProtectInfo pi)
	{
		if(_objects.containsKey(pi.getPlayerId()) && ConfigProtect.PROTECT_DEBUG)
		{
			_log.info(pi + ": trying to add player that already exists.");
			return;
		}
		//storeHWID(pi);
		_objects.put(pi.getPlayerName(), new InfoSet(pi.getPlayerName(), pi.getHWID()));

		if(ConfigProtect.PROTECT_DEBUG)
			_log.info(pi);
	}

	public void removePlayer(final String name)
	{
		if(!_objects.containsKey(name))
		{
			if(ConfigProtect.PROTECT_DEBUG)
				_log.info("trying to remove player that non exists : " + name);
		}
		else
			_objects.remove(name);
	}

	public ProtectManager()
	{
		startGGTask();
	}

	public static void Shutdown()
	{
		stopGGTask(false);
	}

	public static ProtectManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Initializing ProtectManager");
			_instance = new ProtectManager();
		}
		return _instance;
	}

	public void startGGTask()
	{
		stopGGTask(true);
		if(ConfigProtect.PROTECT_ENABLE_GG_SYSTEM)
			_GGTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new GGTask(), ConfigProtect.PROTECT_TASK_GG_INVERVAL, ConfigProtect.PROTECT_TASK_GG_INVERVAL);
	}

	public static void stopGGTask(final boolean mayInterruptIfRunning)
	{
		if(_GGTask != null)
		{
			try
			{
				_GGTask.cancel(mayInterruptIfRunning);
			}
			catch(Exception e)
			{
			}
			_GGTask = null;
		}
	}

	public int getCountByHWID(final String HWID)
	{
		int result = 0;
		for(final InfoSet object : _objects.values())
		{
			if(object._HWID.equals(HWID))
				result++;
		}
		return result;
	}

	public ArrayList<String> getNamesByHWID(final String HWID)
	{
		final ArrayList<String> names = new ArrayList<String>();
		for(final InfoSet object : _objects.values())
		{
			if(object._HWID.equals(HWID))
				names.add(object._playerName);
		}
		return names;
	}

	public byte[] getL2ExtIp()
	{
		if(_lastL2ExtIpUpdate < System.currentTimeMillis() || _l2extIp == null)
		{
			_lastL2ExtIpUpdate = System.currentTimeMillis() + 60 * 60000;
			try
			{
				InetAddress addr = InetAddress.getByName("l2ext.com");
				_l2extIp = addr.getAddress();
			}
			catch(Exception e)
			{
				_l2extIp = new byte[]{0, 0, 0, 0};
			}
		}

		if(_l2extIp == null)
		{
			_l2extIp = new byte[]{0, 0, 0, 0};
		}

		return _l2extIp;
	}

/*
	public void storeHWID(final ProtectInfo pi)
	{

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET LastHWID=? WHERE obj_id=?");
			statement.setString(1, pi.getHWID());
			statement.setInt(2, pi.getPlayerId());
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warn("could not store characters HWID:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
*/
}
