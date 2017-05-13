package ru.l2gw.gameserver.taskmanager;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class AutoSaveManager
{

	protected static org.apache.commons.logging.Log _log = LogFactory.getLog("autoSaveManager");
	private static String _logFile = "autoSaveManager";

	private static AutoSaveManager _instance;
	private static ScheduledFuture<AutoSaveTask> _autoSaveTask = null;

	private class SaveInfo
	{
		public int _objectId;
		public long _lastSaveTime;
		public String _HWID = "";

		public SaveInfo(final int objectId, final String HWID)
		{
			_objectId = objectId;
			_lastSaveTime = System.currentTimeMillis();
			_HWID = HWID;
		}
	}

	private static ConcurrentHashMap<Integer, SaveInfo> _objects = new ConcurrentHashMap<Integer, SaveInfo>();

	class AutoSaveTask implements Runnable
	{
		public void run()
		{
			long time = System.currentTimeMillis();

			for(final SaveInfo object : _objects.values())
				if(time - object._lastSaveTime >= Config.CHAR_SAVE_INTERVAL)
				{
					try
					{
						L2Player pl = L2ObjectsStorage.getPlayer(object._objectId);
						if(!pl.isLogoutStarted() || !pl.isEntering())
							pl.saveCharToDisk();
						object._lastSaveTime = time;
					}
					catch(final Exception e)
					{
						try
						{
							_objects.remove(object._objectId);
							_log.info(object + " remove from task");
						}
						catch(final Exception e2)
						{
							_log.warn(object + " can't remove", e2);
						}
					}
				}

			time = System.currentTimeMillis() - time;

			if(time > Config.TASK_SAVE_INTERVAL)
				_log.info("ALERT! TASK_SAVE_INTERVAL is too small, time to save characters in Queue = " + time);
		}
	}

	public void addPlayer(final L2Player player, final String HWID)
	{
		if(_objects.contains(player.getObjectId()))
			_log.warn(player + " already added!");
		else
			_objects.put(player.getObjectId(), new SaveInfo(player.getObjectId(), HWID));

		_log.info(player + " add hwid: " + HWID);
	}

	public void removePlayer(final L2Player player)
	{
		if(_objects.contains(player.getObjectId()))
			_log.info(player + " no exists");
		else
		{
			_objects.remove(player.getObjectId());
			player.saveCharToDisk();
			_log.info(player + " removed");
		}
	}

	public AutoSaveManager()
	{
		startAutoSaveTask();
	}

	public static void Shutdown()
	{
		stopAutoSaveTask(false);
	}

	public static AutoSaveManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Initializing AutoSaveManager");
			_instance = new AutoSaveManager();
		}
		return _instance;
	}

	public void startAutoSaveTask()
	{
		stopAutoSaveTask(true);
		_autoSaveTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoSaveTask(), Config.TASK_SAVE_INTERVAL, Config.TASK_SAVE_INTERVAL);
	}

	public static void stopAutoSaveTask(final boolean mayInterruptIfRunning)
	{
		if(_autoSaveTask != null)
		{
			_autoSaveTask.cancel(mayInterruptIfRunning);
			_autoSaveTask = null;
		}
	}

	public int getCountByHWID(final String HWID)
	{
		int result = 0;

		for(final SaveInfo object : _objects.values())
		{
			if(object._HWID.equals(HWID))
				result++;
		}

		return result;
	}

	public ArrayList<String> getNamesByHWID(final String HWID)
	{
		final ArrayList<String> names = new ArrayList<String>();

		for(final SaveInfo object : _objects.values())
		{
			if(object._HWID.equals(HWID))
			{
				final L2Player player = L2ObjectsStorage.getPlayer(object._objectId);
				if(player != null)
					names.add(player.getName());
			}
		}

		return names;
	}
}
