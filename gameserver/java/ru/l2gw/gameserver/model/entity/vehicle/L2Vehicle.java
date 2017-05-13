package ru.l2gw.gameserver.model.entity.vehicle;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.gameserver.ai.L2CharacterAI;
import ru.l2gw.gameserver.ai.L2VehicleAI;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.templates.L2CharTemplate;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 07.05.2010 10:43:38
 */
public class L2Vehicle extends L2Character
{
	protected GArray<Location> _broadcastPoints;
	protected GArray<RouteStation> _routeStations;
	protected GCSArray<Integer> _onBoard;
	private int _moveSpeed;
	private int _rotationSpeed;
	protected Location _kickPoint;

	public L2Vehicle(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
	}

	public void addBroadcastPoint(Location point)
	{
		if(_broadcastPoints == null)
			_broadcastPoints = new GArray<Location>();

		_broadcastPoints.add(point);
	}

	public void addRouteStation(RouteStation rs)
	{
		if(_routeStations == null)
			_routeStations = new GArray<RouteStation>();

		rs.setVehicle(this);
		_routeStations.add(rs);
	}

	public GArray<RouteStation> getRouteStations()
	{
		return _routeStations;
	}

	public int getRotationSpeed()
	{
		return _rotationSpeed;
	}

	public void broadcastPacketToPoints(L2GameServerPacket pkt)
	{
		if(_broadcastPoints != null)
			for(Location p : _broadcastPoints)
				for(L2Player player : L2World.getAroundPlayers(p, getReflection(), 0, 20000, 3500))
					if(player != null)
						player.sendPacket(pkt);
	}

	public void sendInfo(L2Player player)
	{
		player.sendPacket(new VehicleInfo(this));
	}

	public void sendOnBoardInfo(L2Player player, L2Player onBoardPlayer)
	{
		if(player != null && _onBoard != null && onBoardPlayer != null && _onBoard.contains(onBoardPlayer.getObjectId()) && player.getVehicle() != this)
		{
			sendInfo(player);
			player.sendPacket(new GetOnVehicle(onBoardPlayer, this, onBoardPlayer.getLocInVehicle()));
		}
	}

	public void setRotationSpeed(int rSpeed)
	{
		_rotationSpeed = rSpeed;
	}

	public void setMoveSpeed(int moveSpeed)
	{
		_moveSpeed = moveSpeed;
	}

	public synchronized void addPlayerOnBoard(L2Player player)
	{
		if(_onBoard == null)
			_onBoard = new GCSArray<Integer>();

		if(!_onBoard.contains(player.getObjectId()))
			_onBoard.add(player.getObjectId());
	}

	public synchronized void removePlayerFromBoard(L2Player player)
	{
		if(_onBoard == null || _onBoard.indexOf(player.getObjectId()) < 0)
			return;

		_onBoard.remove((Integer) player.getObjectId());
	}

	public GCSArray<Integer> getOnBoardPlayer()
	{
		return _onBoard;
	}

	public void oustPlayer(L2Player player, Location loc)
	{
		if(_onBoard == null || _onBoard.indexOf(player.getObjectId()) < 0)
			return;

		removePlayerFromBoard(player);
		player.setVehicle(null);
		player.sendPacket(new GetOffVehicle(player, this, loc));
	}

	public void updateOnBoardPlayers()
	{
		if(_onBoard == null || _onBoard.size() < 1)
			return;

		for(int objectId : _onBoard)
		{
			L2Player player = L2ObjectsStorage.getPlayer(objectId);
			if(player == null || player.getVehicle() != this)
			{
				_onBoard.remove((Integer) objectId);
				continue;
			}

			player.setLocInVehicle(player.getLocInVehicle());
		}
	}

	public void setKickPoint(Location loc)
	{
		_kickPoint = loc;
		if(_onBoard != null)
			for(int objectId : _onBoard)
			{
				L2Player player = L2ObjectsStorage.getPlayer(objectId);
				if(player != null && player.getVehicle() == this)
				{
					player.setStablePoint(loc);
					player.sendPacket(new VehicleStarted(this, 0));
				}
			}
	}

	public Location getKickPoint()
	{
		return _kickPoint;
	}

	@Override
	public void stopMove()
	{
		prevDestination = null;
		if(isMoving)
		{
			isMoving = false;
			setXYZ(getX(), getY(), getZ(), false);
			broadcastPacket(new VehicleInfo(this));
		}
	}

	@Override
	public void setXYZ(int x, int y, int z, boolean move)
	{
		super.setXYZ(x, y, z, move);
		updateOnBoardPlayers();
	}


	@Override
	public int getMoveTickInterval()
	{
		return 500;
	}

	@Override
	public float getMoveSpeed()
	{
		return _moveSpeed;
	}

	@Override
	public L2CharacterAI getAI()
	{
		if(_ai == null)
			_ai = new L2VehicleAI(this);
		return _ai;
	}

	@Override
	public void broadcastMove()
	{
		broadcastPacket(new VehicleInfo(this));
		broadcastPacket(new VehicleDeparture(this));
	}

	@Override
	public void spawnMe()
	{
		if(isTeleporting())
			super.spawnMe();
		else
		{
			if(getRouteStations() == null || getRouteStations().size() < 2)
			{
				_log.info(this + " can't spawn vehicle, no route!");
				return;
			}

			Location startPoint = getRouteStations().get(0).getPoint();
			Location endPoint = getRouteStations().get(getRouteStations().size() - 1).getPoint();
			setXYZInvisible(startPoint.getX(), startPoint.getY(), startPoint.getZ());
			setHeading(startPoint.getHeading() != 0 ? startPoint.getHeading() : (int) (Math.atan2(endPoint.getY() - startPoint.getY(), endPoint.getX() - startPoint.getX()) * HEADINGS_IN_PI) + 32768);
			super.spawnMe();
			((L2VehicleAI) getAI()).doTask();
		}
	}

	@Override
	public byte getLevel()
	{
		return 85;
	}

	@Override
	public void updateAbnormalEffect()
	{}

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean message)
	{
		return false;
	}

	public boolean isManualControlled()
	{
		return false;
	}

	public int getHelmId()
	{
		return 0;
	}

	public L2AirShipHelm getHelm()
	{
		return null;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[name=" + getName() + ";objectId=" + getObjectId() + "] ";
	}
}
