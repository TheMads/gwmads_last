package ru.l2gw.gameserver.model.entity.instance;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.instancemanager.ZoneManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.npcmaker.DefaultMaker;
import ru.l2gw.gameserver.model.playerSubOrders.UserVar;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.DoorTable;
import ru.l2gw.gameserver.tables.MapRegionTable;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.SpawnTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author: rage
 * @date: 25.07.2009 13:02:47
 */
public class Instance
{
	public static final Log _log = LogFactory.getLog("instances");

	protected final InstanceTemplate _template;
	protected List<Integer> _members;
	protected List<L2Spawn> _spawns;
	protected List<L2DoorInstance> _doors;
	protected final int _reflection;
	protected ScheduledFuture<?> _endTask;
	protected ScheduledFuture<?> _closeTask;
	protected long _endTime;
	protected boolean _terminate;
	protected boolean _shutdown = false;
	private int totalPlayers = 0;
	private List<String> _spawnedEvents;
	protected HashMap<String, DefaultMaker> _makers;
	protected long _noUserTimeout = -1;
	protected InZoneListener inZoneListener;

	public Instance(InstanceTemplate template, int reflection)
	{
		_template = template;
		_reflection = reflection;
		_members = new FastList<>();
		_spawns = new FastList<>();
		_doors = new FastList<>();
		_spawnedEvents = new FastList<>();
		if(template.getMakers() != null)
		{
			_makers = new HashMap<>(template.getMakers().size());
			for(DefaultMaker dm : template.getMakers().values())
			{
				DefaultMaker dm0 = dm.clone();
				if(dm0 == null)
					_log.warn(this + " null maker: " + dm);
				else
				{
					dm0.setReflectionId(reflection);
					_makers.put(dm0.name, dm0);
				}
			}
		}
	}

	public void addMembers(List<L2Player> party)
	{
		for(L2Player player : party)
			if(player != null)
			{
				if(Config.DEBUG_INSTANCES)
					_log.info(this + " add member: " + player + " objectId: " + player.getObjectId());
				_members.add(player.getObjectId());
			}
	}

	public void startInstance()
	{
		if(Config.DEBUG_INSTANCES)
			_log.info(this + " start instance");
		try
		{
			if(_template.getAreaList() != null)
			{
				for(String zoneName : _template.getAreaList())
				{
					L2Zone zone = ZoneManager.getInstance().getZoneByName(zoneName);
					if(zone != null)
						zone.setActive(true, _reflection);
					else
						_log.warn(this + " no zone: " + zoneName);
				}
			}

			for(InstanceSpawn is : _template.getSpawns())
			{
				if(is.event != null)
				{
					GArray<L2Spawn> list = SpawnTable.getInstance().getEventSpawn(is.event, this);
					for(L2Spawn spawn : list)
					{
						if(is.delay > 0)
							ThreadPoolManager.getInstance().scheduleGeneral(new DelayedSpawn(is, spawn), is.delay * 1000);
						else
							spawn.init();
						_spawns.add(spawn);
					}
					_spawnedEvents.add(is.event);
				}
				else
				{
					int npcId = is.getNpcId();
					L2NpcTemplate template = NpcTable.getTemplate(npcId);
					int a = 0;

					if(is.count > 1 && is.radius > 0)
						a = 360 / (is.count - 1);

					if(is.location > 0)
						try
						{
							L2Spawn spawn = new L2Spawn(template);
							spawn.setAmount(is.count);
							spawn.setLocation(is.location);
							spawn.setRespawnDelay(is.respawn);
							spawn.setReflection(_reflection);

							spawn.setInstance(this);

							if(is.delay > 0)
								ThreadPoolManager.getInstance().scheduleGeneral(new DelayedSpawn(is, spawn), is.delay * 1000);
							else if(is.respawn > 0)
								spawn.init();
							else
							{
								spawn.init();
								spawn.stopRespawn();
							}
							_spawns.add(spawn);
						}
						catch(Exception e)
						{
							_log.warn(this + " can't create spawns " + e + " npcId: " + npcId);
						}
					else
						for(int i = 0; i < is.count; i++)
						{
							try
							{
								L2Spawn spawn = new L2Spawn(template);
								spawn.setAmount(1);
								spawn.setLoc(is.radius > 0 ? i == 0 ? is.loc : Util.getPointInRadius(is.loc, is.radius, a * (i - 1)) : is.loc);

								spawn.setRespawnDelay(is.respawn);
								spawn.setReflection(_reflection);

								spawn.setInstance(this);

								if(is.delay > 0)
									ThreadPoolManager.getInstance().scheduleGeneral(new DelayedSpawn(is, spawn), is.delay * 1000);
								else if(is.respawn > 0)
									spawn.init();
								else
								{
									spawn.init();
									spawn.stopRespawn();
								}
								_spawns.add(spawn);
							}
							catch(Exception e)
							{
								_log.warn(this + " can't create spawns " + e + " npcId: " + npcId);
							}
						}
				}
			}

			for(Integer doorId : _template.getDoors().keySet())
			{
				L2DoorInstance door = DoorTable.getInstance().getDoor(doorId);
				if(door == null)
				{
					_log.warn(this + " no door id: " + doorId);
					continue;
				}

				L2DoorInstance d = new L2DoorInstance(IdFactory.getInstance().getNextId(), door);
				d.setXYZInvisible(door.getX(), door.getY(), door.getZ());
				d.setReflection(_reflection);
				d.setCurrentHpMp(d.getMaxHp(), d.getMaxMp());
				d.setOpen(false);
				d.spawnMe();
				if(Config.DEBUG_INSTANCES)
					_log.info(this + " spawn door: " + d + " ref: " + d.getReflection());
				if(_template.getDoors().get(doorId))
					d.openMe();
				else
					d.closeMe();
				_doors.add(d);
			}

			if(_makers != null)
				for(DefaultMaker dm : _makers.values())
					dm.onInstanceZoneEvent(this, 1);
		}
		catch(Throwable e)
		{
			_log.warn(this + " can't create spawns " + e);
			e.printStackTrace();
		}

		_endTime = System.currentTimeMillis() + _template.getTimeLimit();
		int[] time = calcTimeForEndTask((int) (_template.getTimeLimit() / 1000));
		_endTask = ThreadPoolManager.getInstance().scheduleGeneral(new EndTask(time[1]), time[0] * 1000L);

		if(inZoneListener != null)
			inZoneListener.onStartInstance(getPlayersInside());
	}

	public void spawnEvent(String event)
	{
		if(!_spawnedEvents.contains(event))
		{
			if(_spawns == null)
				return;

			if(Config.DEBUG_INSTANCES)
				_log.info(this + " spawnEvent: " + event);
			GArray<L2Spawn> list = SpawnTable.getInstance().getEventSpawn(event, this);
			if(list == null)
			{
				_log.info(this + " no spawn event: " + event);
				return;
			}
			_spawnedEvents.add(event);
			for(L2Spawn spawn : list)
			{
				spawn.init();
				_spawns.add(spawn);
			}
		}
	}

	public void stopEventSpawn(String event, boolean despawn)
	{
		if(_spawnedEvents.contains(event))
		{
			if(Config.DEBUG_INSTANCES)
				_log.info(this + " stopEventSpawn: " + event + " despawn: " + despawn);

			for(L2Spawn spawn : _spawns)
				if(event.equals(spawn.getEventName()))
				{
					spawn.stopRespawn();
					if(despawn)
						spawn.despawnAll();
				}
		}
	}
	
	protected int[] calcTimeForEndTask(int time)
	{
		int[] ret = new int[2];
		ret[0] = time;
		if(time >= 600)
		{
			ret[0] = time - 600;
			ret[1] = 10;
		}
		else if(time >= 300)
		{
			ret[0] = time - 300;
			ret[1] = 5;
		}
		else if(time >= 60)
		{
			ret[0] = time - 60;
			ret[1] = 1;
		}

		if(Config.DEBUG_INSTANCES)
			_log.info(this + " cakcTimeForEndTask: sec: " + time + " " + ret[0] + " " + ret[1]);
		return ret;
	}

	public void successEnd()
	{
		if(Config.DEBUG_INSTANCES)
			_log.info(this + " successEnd. " + _terminate);

		_terminate = true;
		for(Integer objectId : _members)
		{
			L2Player player = L2ObjectsStorage.getPlayer(objectId);
			long nextTime = _template.getNextTimeUsage(Config.PREMIUM_RESET_KAMA && player != null && player.isPremiumEnabled());
			if(Config.DEBUG_INSTANCES)
				_log.info(this + " successEnd for: " + (player != null ? player : objectId) + " nextTime: " + new Date(nextTime));

			if(nextTime > System.currentTimeMillis())
			{
			    if(player != null)
					player.setVar("instance-" + _template.getType(), String.valueOf(_template.getId()), (int) (nextTime / 1000));
				else
					L2Player.saveUserVar(objectId, new UserVar("instance-" + _template.getType(), String.valueOf(_template.getId()), nextTime));
			}
		}

		if(_endTask != null)
			_endTask.cancel(true);

		_endTime = System.currentTimeMillis() + _template.getCoolTime();
		int[] time = calcTimeForEndTask((int) (_template.getCoolTime() / 1000));
		if(Config.DEBUG_INSTANCES)
			_log.info(this + " successEnd schedule end task for: " + time[1] + " " + time[0]);
		_endTask = ThreadPoolManager.getInstance().scheduleGeneral(new EndTask(time[1]), time[0] * 1000L);

		if(inZoneListener != null)
			inZoneListener.onSuccessEnd(getPlayersInside());
	}

	public GArray<L2Player> getPlayersInside()
	{
		GArray<L2Player> list = new GArray<L2Player>();
		for(Integer objectId : _members)
		{
			L2Player player = L2ObjectsStorage.getPlayer(objectId);
			if(player != null && player.getReflection() == _reflection && (_template.getZone() == null || _template.getZone().isInsideZone(player)))
				list.add(player);
		}

		return list;
	}

	public void stopInstance()
	{
		if(Config.DEBUG_INSTANCES)
			_log.info(this + " stopInstance: " + _shutdown);

		if(_shutdown)
			return;

		synchronized(this)
		{
			_shutdown = true;
		}

		for(L2Spawn spawn : _spawns)
		{
			if(spawn != null)
			{
				spawn.stopRespawn();
				if(Config.DEBUG_INSTANCES)
					_log.info(this + " stopInstance: despawn: " + spawn.getLastSpawn());
				spawn.despawnAll();
			}
			else
			{
				_log.info(this + " stopInstance: despawn: spawn is null");
			}
		}

		_spawns = null;

		for(L2DoorInstance door : _doors)
		{
			if(Config.DEBUG_INSTANCES)
				_log.info(this + " stopInstance: decay: " + door);
			door.decayMe();
		}

		_doors = null;

		if(_makers != null)
			for(DefaultMaker dm : _makers.values())
				dm.onInstanceZoneEvent(this, 0);

		_makers = null;

		if(inZoneListener != null)
			inZoneListener.onStopInstance(getPlayersInside());

		for(Integer objectId : _members)
		{
			L2Player player = L2ObjectsStorage.getPlayer(objectId);
			if(player != null && !player.isDeleting() && !player.isTeleporting() && player.getReflection() == _reflection && (_template.getZone() == null || _template.getZone().isInsideZone(player)))
			{
				if(Config.DEBUG_INSTANCES)
					_log.info(this + " stopInstance: teleport " + player);
				player.teleToLocation(MapRegionTable.getInstance().getTeleToLocation(player, MapRegionTable.TeleportWhereType.ClosestTown), 0);
			}
		}

		if(_template.getAreaList() != null)
		{
			if(Config.DEBUG_INSTANCES)
				_log.info(this + " stopInstance: area off");

			for(String zoneName : _template.getAreaList())
			{
				L2Zone zone = ZoneManager.getInstance().getZoneByName(zoneName);
				if(zone != null)
					zone.setActive(false, _reflection);
			}
		}

		if(Config.DEBUG_INSTANCES)
			_log.info(this + " stopInstance: remove instance.");
		InstanceManager.getInstance().removeInstance(_template.getId(), _reflection);

		if(_endTask != null)
			_endTask.cancel(true);

		if(_closeTask != null)
			_closeTask.cancel(true);
	}

	public void rescheduleEndTask(int sec)
	{
		if(Config.DEBUG_INSTANCES)
			_log.info(this + " rescheduleEndTask: for: " + sec);
		_terminate = true;
		if(_endTask != null)
		{
			if((_endTime - System.currentTimeMillis()) / 1000 < sec)
				return;
			_endTask.cancel(true);
		}

		_endTime = System.currentTimeMillis() + sec * 1000;
		int[] time = calcTimeForEndTask(sec);
		_endTask = ThreadPoolManager.getInstance().scheduleGeneral(new EndTask(time[1]), time[0] * 1000L);
	}

	public long getTimeLeft()
	{
		return (_endTime - System.currentTimeMillis()) / 1000;
	}

	public void announceToPlayers(L2GameServerPacket gp)
	{
		for(Integer objectId : _members)
		{
			L2Player player = L2ObjectsStorage.getPlayer(objectId);
			if(player != null)
				player.sendPacket(gp);
		}
	}

	public L2NpcInstance addSpawn(int npcId, Location loc, int respawn)
	{
		try
		{
			L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(npcId));
			spawn.setAmount(1);
			spawn.setLoc(loc);
			spawn.setRespawnDelay(respawn);
			spawn.setReflection(_reflection);
			spawn.setInstance(this);

			spawn.init();
			if(respawn < 1)
				spawn.stopRespawn();

			if(Config.DEBUG_INSTANCES)
				_log.info(this + " addSpawn: " + spawn.getLastSpawn());

			if(_spawns != null)
				_spawns.add(spawn);
			else if(Config.DEBUG_INSTANCES)
				_log.info(this + " spawns is null WTF??? " + spawn.getLastSpawn());

			return spawn.getLastSpawn();
		}
		catch(Exception e)
		{
			_log.warn(this + " can't create spawns " + e + " npcId: " + npcId);
			e.printStackTrace();
		}
		return null;
	}

	public List<L2Spawn> getInstanceSpawns()
	{
		return _spawns;
	}

	public List<L2DoorInstance> getDoors()
	{
		return _doors;
	}

	protected class EndTask implements Runnable
	{
		private int _min;

		public EndTask(int min)
		{
			_min = min;
		}

		public void run()
		{
			if(_min > 0)
			{
				if(_terminate)
					announceToPlayers(new SystemMessage(SystemMessage.THIS_INSTANCE_ZONE_WILL_BE_TERMINATED_IN_S1_MINUTES_YOU_WILL_BE_FORCED_OUT_OF_THE_DUNGEON_WHEN_THE_TIME_EXPIRES).addNumber(_min));
				else
					announceToPlayers(new SystemMessage(SystemMessage.THIS_DUNGEON_WILL_EXPIRE_IN_S1_MINUTES_YOU_WILL_BE_FORCED_OUT_OF_THE_DUNGEON_WHEN_THE_TIME_EXPIRES).addNumber(_min));

				long time;
				if(_min == 10)
				{
					_min = 5;
					time = 300;
				}
				else if(_min == 5)
				{
					_min = 1;
					time = 240;
				}
				else
				{
					_min = 0;
					time = 60;
				}

				if(Config.DEBUG_INSTANCES)
					_log.info(Instance.this + " next min: " + _min + " schedule for: " + time + " sec.");
				_endTask = ThreadPoolManager.getInstance().scheduleGeneral(this, time * 1000L);
				return;
			}

			stopInstance();
		}
	}

	private class DelayedSpawn implements Runnable
	{
		private final InstanceSpawn _is;
		private L2Spawn _spawn;

		public DelayedSpawn(InstanceSpawn is, L2Spawn spawn)
		{
			_is = is;
			_spawn = spawn;
		}

		public void run()
		{
			if(_is.respawn > 0)
				_spawn.init();
			else
			{
				_spawn.init();
				_spawn.stopRespawn();
			}
		}
	}

	public boolean isInside(int objectId)
	{
		return _members.contains(objectId);
	}

	public List<Integer> getMembers()
	{
		return _members;
	}

	public InstanceTemplate getTemplate()
	{
		return _template;
	}

	public int getReflection()
	{
		return _reflection;
	}

	public Location getStartLoc()
	{
		return _template.getStartLoc();
	}

	public void onPlayerExit(L2Player player)
	{
		if(Config.DEBUG_INSTANCES)
			_log.info(this + " player exit: " + player);
		totalPlayers--;
		if(getNoUserTimeout() >=0 && totalPlayers < 1)
		{
			if(getNoUserTimeout() ==0)
			{
				_terminate = true;
				stopInstance();
			}
			else if(_closeTask == null)
			{
				_closeTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					public void run()
					{
						_terminate = true;
						stopInstance();
					}
				}, getNoUserTimeout());
			}
		}

		if(_template.getDispelOnExit() != null)
		{
			for(int skillId : _template.getDispelOnExit())
			{
				player.stopEffect(skillId);
				L2Summon summon = player.getPet();
				if(summon != null)
					summon.stopEffect(skillId);
			}
		}

		if(inZoneListener != null)
			inZoneListener.onPlayerExit(player);
	}

	public long getNoUserTimeout()
	{
		if(_noUserTimeout < 0)
			return _template.getNoUserTimeout();

		return _noUserTimeout;
	}

	public void setNoUserTimeout(long timeout)
	{
		_noUserTimeout = timeout;
	}

	public void onPlayerEnter(L2Player player)
	{
		if(Config.DEBUG_INSTANCES)
			_log.info(this + " player enter: " + player);
		totalPlayers++;
		if(_closeTask != null)
		{
			_closeTask.cancel(false);
			_closeTask = null;
		}

		if(inZoneListener != null)
			inZoneListener.onPlayerEnter(player);
	}

	public void sendScriptEvent(int eventId, Object arg1, Object arg2)
	{
		for(L2Spawn spawn : _spawns)
			for(L2NpcInstance npc : spawn.getAllSpawned())
				if(!npc.isDead() && !npc.isDecayed())
					npc.getAI().notifyEvent(CtrlEvent.EVT_SCRIPT_EVENT, eventId, arg1, arg2);
	}

	public void notifyKill(L2Character cha, L2Player killer)
	{
	}

	public void notifyAttacked(L2Character cha, L2Player attacker)
	{
	}

	public void notifyDecayd(L2NpcInstance npc)
	{
	}

	public void notifyEvent(String event, L2Character cha, L2Player player)
	{
	}

	public DefaultMaker getMaker(String maker)
	{
		if(_makers == null)
			return null;

		return _makers.get(maker);
	}

	public void openCloseDoor(String doorName, int close)
	{
		if(doorName == null || doorName.isEmpty())
			return;

		for(L2DoorInstance door : _doors)
			if(doorName.equalsIgnoreCase(door.getDoorName()))
			{
				if(close == 0)
				{
					door.openMe();
					door.onOpen();
				}
				else
					door.closeMe();
				break;
			}
	}

	public void markRestriction()
	{
		for(Integer objectId : _members)
		{
			L2Player player = L2ObjectsStorage.getPlayer(objectId);
			long nextTime = _template.getNextTimeUsage(Config.PREMIUM_RESET_KAMA && player != null && player.isPremiumEnabled());
			if(Config.DEBUG_INSTANCES)
				_log.info(this + " markRestriction for: " + (player != null ? player : objectId) + " nextTime: " + new Date(nextTime));

			if(nextTime > System.currentTimeMillis())
			{
			    if(player != null)
					player.setVar("instance-" + _template.getType(), String.valueOf(_template.getId()), (int) (nextTime / 1000));
				else
					L2Player.saveUserVar(objectId, new UserVar("instance-" + _template.getType(), String.valueOf(_template.getId()), nextTime));
			}
		}
	}

	public Location getEndPos()
	{
		return _template.getEndLoc();
	}

	public InZoneListener getInZoneListener()
	{
		return inZoneListener;
	}

	public void setInZoneListener(InZoneListener inZoneListener)
	{
		this.inZoneListener = inZoneListener;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[id=" + _template.getId() + ";refId=" + _reflection + ";name=" + _template.getName() + "]";
	}
}
