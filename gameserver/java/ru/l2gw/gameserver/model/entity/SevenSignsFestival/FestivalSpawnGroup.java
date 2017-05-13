package ru.l2gw.gameserver.model.entity.SevenSignsFestival;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.instances.L2FestivalMonsterInstance;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Location;
import ru.l2gw.commons.math.Rnd;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author rage
 * @date 08.06.2009 15:41:50
 */
public class FestivalSpawnGroup
{
	private static Log _log = LogFactory.getLog("sevensigns");
	private final FestivalSpawnGroupTemplate _template;
	private Map<Integer, List<L2FestivalMonsterInstance>> _mobInstances;
	private List<Location> _spawns;
	private int _respawnCount;
	private int _currentSpawnCount = 0;
	private int _respawnDelay;
	private Map<Integer, Future<?>> _spawnTasks;
	private Festival _festival;

	public FestivalSpawnGroup(Festival festival, FestivalSpawnGroupTemplate template, List<StatsSet> mobs, List<Location> spawns)
	{
		_template = template;
		_spawns = spawns;
		_festival = festival;
		_respawnCount = _template.minSpawn;
		_respawnDelay = _template.initialRespawnDelay * 1000;
		_spawnTasks = new FastMap<Integer, Future<?>>();

		_mobInstances = new FastMap<Integer, List<L2FestivalMonsterInstance>>();

		for(int i = 0; i < _template.maxSpawn; i++)
		{
			List<L2FestivalMonsterInstance> list = new FastList<L2FestivalMonsterInstance>();

			for(StatsSet mobSet : mobs)
			{
				L2NpcTemplate tpl = NpcTable.getTemplate(mobSet.getInteger("npcId"));
				if(tpl == null)
				{
					_log.warn("FestivalSpawnGroup: has no npc template for npcId: " + mobSet.getInteger("npcId"));
					continue;
				}

				L2FestivalMonsterInstance mob = new L2FestivalMonsterInstance(IdFactory.getInstance().getNextId(), tpl, 0, 0, 0, 0);
				mob.setOfferingCount(mobSet.getInteger("items"));
				mob.setFestivalId(mobSet.getInteger("festivalId"));
				mob.setDecayed(true);
				mob.setSubGroupId(i);
				list.add(mob);
			}
			_mobInstances.put(i, list);
		}
	}

	public int getMinSpawnCount()
	{
		return _template.minSpawn;
	}

	public int getMaxSpawnCount()
	{
		return _template.maxSpawn;
	}

	public void startRespawn()
	{
		for(int i = 0; i <_respawnCount; i++)
		{
			if(_template.initialDelay > 0)
				_spawnTasks.put(i, ThreadPoolManager.getInstance().scheduleGeneral(new RespawnTask(_mobInstances.get(i)), _template.initialDelay * 1000));
			else
				initializeGroup(_mobInstances.get(i));
		}
		_currentSpawnCount = _respawnCount;
	}

	public void initializeGroup(List<L2FestivalMonsterInstance> list)
	{
		if(_template.randomSpawn)
		{
			L2FestivalMonsterInstance mob = list.get(Rnd.get(list.size()));

			mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());

			mob.stopAllEffects();
			mob.setShowSpawnAnimation(true);

			// Link the L2NpcInstance to this L2Spawn
			mob.setSpawn(null);
			mob.setSpawnGroup(this);

			Location newLoc = _spawns.get(Rnd.get(_spawns.size()));
			// save spawned points
			mob.setSpawnedLoc(newLoc);

			// Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world as a visible object
			mob.spawnMe(newLoc);

			// Launch the action onSpawn for the L2NpcInstance
			mob.onSpawn();

			L2Spawn.notifyNpcSpawned(mob);
		}
		else
		{
			Location newLoc = null;
			int c = 0;

			if(_template.randomCoord)
			{
				newLoc = _spawns.get(Rnd.get(_spawns.size()));
				if(Config.DEBUG)
					_log.info(this + " spawn at random: " + newLoc);
			}
			else
				c = Rnd.get(_spawns.size() / list.size()) * list.size();

			for(L2FestivalMonsterInstance mob : list)
			{
				mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
				mob.setShowSpawnAnimation(true);

				mob.stopAllEffects();

				// Link the L2NpcInstance to this L2Spawn
				mob.setSpawnGroup(this);
				mob.setSpawn(null);
				
				if(!_template.randomCoord)
				{
					if(c < _spawns.size())
					{
						newLoc = _spawns.get(c);
						c++;
					}
					else
					{
						c = 0;
						newLoc = _spawns.get(c);
					}
					if(Config.DEBUG)
						_log.info(this + " spawn at fixed: " + newLoc + " c: " + c);
				}

				// save spawned points
				mob.setSpawnedLoc(newLoc);

				// Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world as a visible object
				mob.spawnMe(newLoc);

				// Launch the action onSpawn for the L2NpcInstance
				mob.onSpawn();

				L2Spawn.notifyNpcSpawned(mob);
			}
		}
	}

	private class RespawnTask implements Runnable
	{
		List<L2FestivalMonsterInstance> _list;

		public RespawnTask(List<L2FestivalMonsterInstance> list)
		{
			_list = list;
		}

		public void run()
		{
			if(!_festival.isStarted())
				return;

			for(L2FestivalMonsterInstance mob : _list)
				mob.refreshID();
			initializeGroup(_list);
		}
	}

	public void stopRespawn()
	{
		if(Config.DEBUG)
			_log.info(this + " stop respawn, spawn tasks: " + _spawns.size());
		for(Future<?> task : _spawnTasks.values())
		{
			if(task != null)
				task.cancel(true);
		}

		_spawnTasks.clear();

		for(List<L2FestivalMonsterInstance> list : _mobInstances.values())
		{
			for(L2FestivalMonsterInstance mob : list)
			{
				mob.setDecayed(true);
				mob.deleteMe();
			}
		}

		_respawnCount = _template.minSpawn;
		_currentSpawnCount = 0;
	}

	public void npcDecayed(L2FestivalMonsterInstance npc)
	{
		if(!_festival.isStarted())
			return;

		_respawnDelay = (int)(_template.finalRespawnDelay * 1000 + (1 - _festival.getFestivalProgress()) * ((_template.initialRespawnDelay - _template.finalRespawnDelay) * 1000));
		if(_respawnDelay < _template.finalRespawnDelay * 1000)
			_respawnDelay = _template.finalRespawnDelay * 1000;

		if(_template.minSpawn != _template.maxSpawn)
		{
			int respawnCount = (int)Math.round(_template.minSpawn + _festival.getFestivalProgress() * (_template.maxSpawn - _template.minSpawn));

			if(Config.DEBUG)
				_log.info(this + " current spawn count: " + _respawnCount + " new spawn count: " + respawnCount);

			if(respawnCount > _template.maxSpawn)
				respawnCount = _template.maxSpawn;

			if(_respawnCount < respawnCount)
			{
				for(int i=_respawnCount; i < respawnCount; i++)
				{
					if(Config.DEBUG)
						_log.info(this + " spawn new subId: "+i);
					initializeGroup(_mobInstances.get(i));
				}
				_respawnCount = respawnCount;
			}
		}

		List<L2FestivalMonsterInstance> list = _mobInstances.get(npc.getSubGroupId());
		if(list != null)
		{
			boolean respawn = true;
			for(L2FestivalMonsterInstance mob : list)
				if(!mob.isDecayed())
				{
					respawn = false;
					break;
				}

			if(respawn)
			{
				if(Config.DEBUG)
					_log.info(this + " Schedule respawn subId: " + npc.getSubGroupId() + " to " + _respawnDelay);

				Future<?> task = _spawnTasks.remove(npc.getSubGroupId());

				if(task != null && !task.isDone())
				{
					if(Config.DEBUG)
						_log.info(this + " try to reschedulet respawn for subId: "+npc.getSubGroupId());
					task.cancel(true);
				}

				_spawnTasks.put(npc.getSubGroupId(), ThreadPoolManager.getInstance().scheduleGeneral(new RespawnTask(list), _respawnDelay));
			}	
		}
	}

	@Override
	public String toString()
	{
		return "FestivalSpawnGroup[" + _template.groupId + "]";
	}
}
