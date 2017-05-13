package ru.l2gw.gameserver.model.quest;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Player;

public class QuestPcSpawnManager
{
	public class ScheduleTimerTask implements Runnable
	{
		public void run()
		{
			try
			{
				cleanUp();
				ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), 60000);
			}
			catch(Throwable t)
			{}
		}
	}

	private static QuestPcSpawnManager _instance;

	/** Return global instance of QuestPcSpawnManager */
	public static QuestPcSpawnManager getInstance()
	{
		if(_instance == null)
			_instance = new QuestPcSpawnManager();
		return _instance;
	}

	private GArray<QuestPcSpawn> _pcSpawns = new GArray<QuestPcSpawn>();

	public QuestPcSpawnManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), 60000);
	}

	/**
	 * Remove all spawn for all player instance that does not exist
	 */
	public void cleanUp()
	{
		for(int i = getPcSpawns().size() - 1; i >= 0; i--)
			if(getPcSpawns().get(i).getPlayer() == null)
			{
				removeAllSpawn(getPcSpawns().get(i));
				getPcSpawns().remove(i);
			}
	}

	/**
	 * Return true of contain player instance
	 */
	public boolean contains(L2Player player)
	{
		for(QuestPcSpawn qps : _pcSpawns)
		{
			L2Player owner = qps.getPlayer();
			if(owner != null && owner.getObjectId() == player.getObjectId())
				return true;
		}
		return false;
	}

	/** Return quest pc spawn for specified player instance */
	public QuestPcSpawn getPcSpawn(L2Player player)
	{
		for(QuestPcSpawn qps : _pcSpawns)
		{
			L2Player owner = qps.getPlayer();
			if(owner != null && owner.getObjectId() == player.getObjectId())
				return qps;
		}
		QuestPcSpawn qps =  new QuestPcSpawn(player);
		_pcSpawns.add(qps);
		return qps;
	}

	/** Return all quest pc spawn */
	public GArray<QuestPcSpawn> getPcSpawns()
	{
		return _pcSpawns;
	}

	/**
	 * Remove all spawn from the QuestPcSpawn instance
	 */
	private void removeAllSpawn(QuestPcSpawn pcspawn)
	{
		pcspawn.removeAllSpawn();
	}
}
