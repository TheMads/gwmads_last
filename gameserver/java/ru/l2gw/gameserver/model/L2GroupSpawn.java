package ru.l2gw.gameserver.model;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author rage
 * @date 20.10.2009 15:50:21
 */
public class L2GroupSpawn
{
	private static final Log _log = LogFactory.getLog("groupspawn");
	private Collection<L2Spawn> _spawns;
	private int _respawnDelay;
	private boolean _respawn;
	private int _reflection;
	private int _currentCount = 0;
	private ScheduledFuture<?> _respawnTask;
	private Instance _instance;
	private String _evenet;

	public L2GroupSpawn()
	{
		_spawns = new FastList<L2Spawn>().shared();
	}

	public void setRespawnDelay(int delay)
	{
		_respawnDelay = delay;
		_respawn = delay > 0;
	}

	public void stopRespawn()
	{
		_respawn = false;
		if(_respawnTask != null)
			_respawnTask.cancel(true);
		_respawnTask = null;
	}

	public void startRespawn()
	{
		_respawn = true;
	}

	public void setReflection(int reflection)
	{
		_reflection = reflection;
	}

	public void setInstance(Instance instance)
	{
		_instance = instance;
	}

	public void addSpawn(int npcId, Location loc)
	{
		addSpawn(npcId, loc, 1);
	}

	public void addSpawn(int npcId, Location loc, int amount)
	{
		try
		{
			L2NpcTemplate template = NpcTable.getTemplate(npcId);
			L2Spawn spawn = new L2Spawn(template);
			spawn.setAmount(amount);
			spawn.setLoc(loc);
			spawn.setReflection(_reflection);
			spawn.stopRespawn();
			spawn.setGroupSpawn(this);
			spawn.setInstance(_instance);
			_spawns.add(spawn);
		}
		catch(Exception e)
		{
			_log.warn("L2GroupSpawn: addSpawn error: " + e);
			e.printStackTrace();
		}
	}

	public void addSpawn(L2Spawn spawn)
	{
		if(spawn != null)
		{
			spawn.setReflection(_reflection);
			spawn.setInstance(_instance);
			spawn.setGroupSpawn(this);
			_spawns.add(spawn);
		}
	}

	public void decreaseCount()
	{
		_currentCount--;
		if(_currentCount < 0)
			_currentCount = 0;

		if(_respawn && _currentCount == 0)
		{
			if(_respawnTask != null)
				_respawnTask.cancel(true);

			_respawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new RespawnTask(), _respawnDelay * 1000L);
		}
	}

	public void despawnAll()
	{
		_respawn = false;
		for(L2Spawn spawn : _spawns)
			spawn.despawnAll();
	}

	public List<L2NpcInstance> getAllSpawned()
	{
		List<L2NpcInstance> list = new FastList<L2NpcInstance>();
		for(L2Spawn spawn : _spawns)
			list.addAll(spawn.getAllSpawned());

		return list;
	}

	public boolean isAllDead()
	{
		for(L2NpcInstance npc : getAllSpawned())
			if(npc != null && !npc.isDead())
				return false;

		return true;
	}

	public boolean isAllDecayed()
	{
		for(L2NpcInstance npc : getAllSpawned())
			if(npc != null && !npc.isDecayed())
				return false;

		return true;
	}

	public void doSpawn()
	{
		for(L2Spawn spawn : _spawns)
		{
			spawn.init();
			spawn.stopRespawn();
		}
		_currentCount = _spawns.size();
	}

	public void setEventName(String event)
	{
		_evenet = event;
	}

	public String getEventName()
	{
		return _evenet;
	}

	private class RespawnTask implements Runnable
	{
		public void run()
		{
			if(_respawn)
				doSpawn();
		}
	}
}
