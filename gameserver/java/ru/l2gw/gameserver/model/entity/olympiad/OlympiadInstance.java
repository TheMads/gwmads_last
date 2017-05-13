package ru.l2gw.gameserver.model.entity.olympiad;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2GroupSpawn;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.entity.instance.InstanceTemplate;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.tables.DoorTable;
import ru.l2gw.gameserver.tables.SpawnTable;

/**
 * @author rage
 * @date 29.04.11 12:24
 */
public class OlympiadInstance extends Instance
{
	private int arenaId;
	private OlympiadGame og;
	private L2GroupSpawn buffer;

	public OlympiadInstance(InstanceTemplate template, int reflection)
	{
		super(template, reflection);
		buffer = SpawnTable.getInstance().getEventGroupSpawn("olympiad_manager" + template.getId(), this);
	}

	public boolean isFree()
	{
		return og == null;
	}

	public synchronized void setOlympiadGame(OlympiadGame o)
	{
		og = o;
	}

	public OlympiadGame getOlympiadGame()
	{
		return og;
	}

	public void setArenaId(int id)
	{
		arenaId = id;
	}

	public int getArenaId()
	{
		return arenaId;
	}

	public void broadcastPacket(L2GameServerPacket gsp)
	{
		for(L2Character cha : _template.getZone().getCharacters())
			if(cha.isPlayer() && cha.getReflection() == getReflection())
				cha.sendPacket(gsp);
	}

	@Override
	public void notifyEvent(String event, L2Character cha, L2Player player)
	{
		if("spawn_manager".equals(event))
			buffer.doSpawn();
		else if("despawn_manager".equals(event))
			buffer.despawnAll();
		else if("open_door".equals(event))
			for(L2DoorInstance door : _doors)
				door.openMe();
		else if("close_door".equals(event))
			for(L2DoorInstance door : _doors)
				door.closeMe();
	}

	@Override
	public void startInstance()
	{
		if(Config.DEBUG_INSTANCES)
			_log.info(this + " start instance");
		try
		{
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
		}
		catch(Throwable e)
		{
			_log.warn(this + " can't create spawns " + e);
			e.printStackTrace();
		}
	}

	@Override
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

		buffer.despawnAll();

		for(L2DoorInstance door : _doors)
		{
			if(Config.DEBUG_INSTANCES)
				_log.info(this + " stopInstance: decay: " + door);
			door.decayMe();
		}

		_doors = null;

		for(L2Character cha : _template.getZone().getCharacters())
		{
			if(cha != null && cha.isPlayer() && cha.getReflection() == getReflection())
			{
				L2Player player = (L2Player) cha;
				if(Config.DEBUG_INSTANCES)
					_log.info(this + " stopInstance: teleport " + player);
				if(player.inObserverMode())
					player.leaveOlympiadObserverMode();
				else
				{
					OlympiadGame.prepareTeleportBack(player);
					if(player.getStablePoint() == null)
						player.teleToClosestTown();
					else
					{
						player.teleToLocation(player.getStablePoint(), 0);
						player.setStablePoint(null);
					}
				}
			}
		}

		if(Config.DEBUG_INSTANCES)
			_log.info(this + " stopInstance: remove instance.");
		InstanceManager.getInstance().removeInstance(_template.getId(), _reflection);
	}
}
