package ru.l2gw.gameserver.model.entity.DimensionalRift;

import javolution.util.FastList;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.L2Territory;
import ru.l2gw.gameserver.model.zone.L2RiftRoomZone;
import ru.l2gw.gameserver.serverpackets.Earthquake;
import ru.l2gw.util.Location;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author rage
 * @date 03.09.2009 15:43:52
 */
public class DimensionalRiftRoom
{
	private final FastList<L2Spawn> _roomSpawns;
	private final L2RiftRoomZone _zone;
	private boolean _isBusy;
	private final L2Territory _territory;
	private ScheduledFuture<?> _spawnTask;
	
	public DimensionalRiftRoom(L2RiftRoomZone zone)
	{
		_zone = zone;
		_territory = new L2Territory("rift_room_" + zone.getRoomType() + "_" + zone.getRoomId());
		_territory.add(zone.getMinX() + 128, zone.getMinY() + 128, zone.getMinZ(), zone.getMaxZ());
		_territory.add(zone.getMinX() + 128, zone.getMaxY() - 128, zone.getMinZ(), zone.getMaxZ());
		_territory.add(zone.getMaxX() - 128, zone.getMaxY() - 128, zone.getMinZ(), zone.getMaxZ());
		_territory.add(zone.getMaxX() - 128, zone.getMinY() + 128, zone.getMinZ(), zone.getMaxZ());
		_roomSpawns = new FastList<L2Spawn>();
		_isBusy = false;
	}

	public byte getRoomId()
	{
		return _zone.getRoomId();
	}

	public byte getRoomType()
	{
		return _zone.getRoomType();
	}

	public boolean isBusy()
	{
		return _isBusy;
	}

	public void setIsBusy(boolean busy)
	{
		_isBusy = busy;
	}

	public Location getTeleportLocation()
	{
		return _zone.getTeleportLocation();
	}

	public boolean isBossRoom()
	{
		return _zone.isBossRoom();
	}

	public L2Territory getTerritory()
	{
		return _territory;
	}

	public FastList<L2Spawn> getSpawns()
	{
		return _roomSpawns;
	}

	public void addSpawn(L2Spawn spawn)
	{
		_roomSpawns.add(spawn);
	}

	public void telePlayersToOut()
	{
		_zone.telePlayers(_zone.getSpawn());
	}

	public void telePlayersToNext(DimensionalRiftRoom nextRoom)
	{
		_zone.telePlayers(nextRoom.getTeleportLocation());
	}

	public void earthquake()
	{
		for(L2Character cha : _zone.getCharacters())
			if(cha != null && cha.isPlayer())
				cha.sendPacket(new Earthquake(cha.getLoc(), 15, 5));
	}

	public void start()
	{
		_isBusy = true;

		if(_spawnTask != null)
			_spawnTask.cancel(true);

		_spawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){ public void run(){ spawn(); }}, Config.RIFT_SPAWN_DELAY);

	}

	public void stop()
	{
		if(_spawnTask != null)
			_spawnTask.cancel(true);

		unspawn();

		_isBusy = false;
	}

	private void spawn()
	{
		for(L2Spawn spawn : _roomSpawns)
			spawn.init();
	}

	private void unspawn()
	{
		for(L2Spawn spawn : _roomSpawns)
		{
			spawn.stopRespawn();
			spawn.despawnAll();
		}
	}

	public List<L2Player> getPlayers()
	{
		List<L2Player> ret = new FastList<L2Player>();

		for(L2Character cha : _zone.getCharacters())
			if(cha != null && cha.isPlayer())
				ret.add((cha.getPlayer()));

		return ret;
	}

	public boolean isInside(L2Player player)
	{
		return _zone.isCharacterInZone(player);
	}

	public int getPlayersCount()
	{
		int ret = 0;
		for(L2Character cha : _zone.getCharacters())
			if(cha != null && cha.isPlayer())
				ret++;

		return ret;
	}

	public L2RiftRoomZone getZone()
	{
		return _zone;
	}
}
