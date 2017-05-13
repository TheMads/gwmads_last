package ru.l2gw.gameserver.model.entity.DimensionalRift;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.DimensionalRiftManager;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;
import ru.l2gw.commons.math.Rnd;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

/** 
* @author rage
*/ 
public class DimensionalRift
{
	private L2Party _party;
	private DimensionalRiftRoom _currentRoom;
	private ScheduledFuture<?> _teleportTask;
	private ScheduledFuture<?> _earthquakeTask;
	private boolean _bossKilled = false;
	private final long _endTime;
	private byte _currentJump = 1;
	private boolean _isJumped = false;
	private boolean _manualTeleport = false;

	private final static Log _log = LogFactory.getLog(DimensionalRift.class.getName());

	public DimensionalRift(L2Party party, DimensionalRiftRoom room)
	{
		_endTime = System.currentTimeMillis() + Config.RIFT_AUTO_JUMPS_TIME * Config.RIFT_MAX_JUMPS + 10000;
		party.setDimensionalRift(this);
		_party = party;
		_currentRoom = room;
		Location coords = room.getTeleportLocation();

		for(L2Player p : party.getPartyMembers())
			p.teleToLocation(coords);

		room.start();
		_teleportTask = ThreadPoolManager.getInstance().scheduleGeneral(new RiftTeleportTask(), Config.RIFT_AUTO_JUMPS_TIME);
		_earthquakeTask = ThreadPoolManager.getInstance().scheduleGeneral(new EarthquakeTask(), Config.RIFT_AUTO_JUMPS_TIME - 3000);
	}

	public L2Party getParty()
	{
		return _party;
	}

	public boolean isJumped()
	{
		return _isJumped;
	}

	private class RiftTeleportTask implements Runnable
	{
		public void run()
		{
			_currentJump++;

			if(_party == null || _party.getMemberCount() < 1)
			{
				_currentRoom.telePlayersToOut();
				_currentRoom.stop();
				DimensionalRiftManager.getInstance().removeRift(DimensionalRift.this);
				return;
			}

			boolean isDead = true;
			for(L2Player member : _party.getPartyMembers())
				if(member != null && _currentRoom.isInside(member) && !member.isDead())
				{
					isDead = false;
					break;
				}

			if(isDead || _bossKilled || _currentJump > Config.RIFT_MAX_JUMPS || _currentRoom.getPlayersCount() < 2 || _endTime < System.currentTimeMillis() + Config.RIFT_AUTO_JUMPS_TIME)
			{
				_currentRoom.telePlayersToOut();
				_currentRoom.stop();
				_party.setDimensionalRift(null);
				DimensionalRiftManager.getInstance().removeRift(DimensionalRift.this);
				return;
			}

			List<DimensionalRiftRoom> rooms = DimensionalRiftManager.getInstance().getFreeRooms(_currentRoom.getRoomType(), _currentJump != Config.RIFT_MAX_JUMPS - 1 && !_manualTeleport);
			_manualTeleport = false;

			if(rooms.size() > 0)
			{
				DimensionalRiftRoom nextRoom = rooms.get(Rnd.get(rooms.size()));
				nextRoom.start();
				_currentRoom.telePlayersToNext(nextRoom);
				_currentRoom.stop();
				_currentRoom = nextRoom;

				_teleportTask = ThreadPoolManager.getInstance().scheduleGeneral(new RiftTeleportTask(), Config.RIFT_AUTO_JUMPS_TIME);
				_earthquakeTask = ThreadPoolManager.getInstance().scheduleGeneral(new EarthquakeTask(), Config.RIFT_AUTO_JUMPS_TIME - 3000);
			}
			else
			{
				_log.warn(this + ": has no free rooms, teleport players to out!");
				_currentRoom.telePlayersToOut();
				_currentRoom.stop();
				_party.setDimensionalRift(null);
				DimensionalRiftManager.getInstance().removeRift(DimensionalRift.this);
			}
		}
	}

	private class EarthquakeTask implements Runnable
	{
		public void run()
		{
			_currentRoom.earthquake();
		}
	}

	public void manualTeleport()
	{
		_isJumped = true;

		if(_teleportTask != null)
			_teleportTask.cancel(true);

		if(_earthquakeTask != null)
			_earthquakeTask.cancel(true);

		_manualTeleport = true;

		new RiftTeleportTask().run();
	}

	public void manualExit()
	{
		if(_teleportTask != null)
			_teleportTask.cancel(true);

		if(_earthquakeTask != null)
			_earthquakeTask.cancel(true);

		_currentRoom.telePlayersToOut();
		_currentRoom.stop();
		DimensionalRiftManager.getInstance().removeRift(this);
		_party.setDimensionalRift(null);
	}

	public void rescheduleTeleportTask(int sec)
	{
		if(_teleportTask != null)
			_teleportTask.cancel(true);

		if(_earthquakeTask != null)
			_earthquakeTask.cancel(true);

		_teleportTask = ThreadPoolManager.getInstance().scheduleGeneral(new RiftTeleportTask(), sec * 1000);
		_earthquakeTask = ThreadPoolManager.getInstance().scheduleGeneral(new RiftTeleportTask(), (sec - 3) * 1000);
	}

	public void checkDeath()
	{
		boolean isDead = true;
		for(L2Player player : _currentRoom.getPlayers())
			if(player != null && !player.isDead())
			{
				isDead = false;
				break;
			}

		if(isDead)
			manualExit();
	}

	public void oustMember(String name)
	{
		if(_party != null)
		{
			for(L2Player player : _party.getPartyMembers())
				if(player != null && player.getName().equalsIgnoreCase(name) && _currentRoom.isInside(player))
				{
					player.teleToLocation(_currentRoom.getZone().getSpawn());
					break;
				}
		}
	}

	public void setBossKilled(boolean killed)
	{
		_bossKilled = killed;
	}

	@Override
	public String toString()
	{
		return "DimensionalRift[type=" + _currentRoom.getRoomType() + ";roomId=" + _currentRoom.getRoomId() + "]";	
	}

}
