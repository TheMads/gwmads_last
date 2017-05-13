package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.serverpackets.ExValidateLocationInAirShip;
import ru.l2gw.gameserver.serverpackets.ValidateLocation;
import ru.l2gw.gameserver.serverpackets.ValidateLocationInVehicle;
import ru.l2gw.util.Location;

public class ValidatePosition extends L2GameClientPacket
{
	private Location _loc = new Location(0, 0, 0);
	private int _vehicleObjId;
	private Location _lastClientPosition;
	private Location _lastServerPosition;

	/**
	 * packet type id 0x48
	 * format:		cddddd
	 */
	@Override
	public void readImpl()
	{
		_loc.setX(readD());
		_loc.setY(readD());
		_loc.setZ(readD());
		_loc.setH(readD());
		_vehicleObjId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isTeleporting() || player.inObserverMode())
			return;

		_lastClientPosition = player.getLastClientPosition();
		_lastServerPosition = player.getLastServerPosition();

		if(_lastClientPosition == null)
			_lastClientPosition = player.getLoc();
		if(_lastServerPosition == null)
			_lastServerPosition = player.getLoc();

		if(player.isAirshipCaptain())
			return;

		if(!player.isInBoat() && player.getX() == 0 && player.getY() == 0 && player.getZ() == 0)
		{
			correctPosition(player);
			_loc = player.getLoc();
		}
		double _diff = _loc.distance3D(player.getLoc());

		L2Vehicle vehicle = player.getVehicle();

		if(vehicle != null && _vehicleObjId == vehicle.getObjectId())
		{
			_diff = _loc.distance(player.getLocInVehicle());
			if(vehicle.isAirShip())
				player.setHeading(_loc.getHeading());
			if(_diff > 500)
				player.sendPacket(vehicle.isAirShip() ? new ExValidateLocationInAirShip(player) : new ValidateLocationInVehicle(player));
			return;
		}

		if(player.isInFlyingTransform() && (_loc.getZ() < -1050 || _loc.getZ() > 6050))
		{
			player.teleToLocation(player.getLoc().setZ(Math.min(6000, Math.max(-1000, _loc.getZ()))));
			return;
		}

		if(_loc.getZ() < -16384 || _loc.getZ() > 15000)
			correctPosition(player);
		else if(_diff > 1000 && !player.isFalling())
		{
			//player.sendMessage("diff: " + String.format("%.2f", _diff));
			if(player.isFloating())
				player.teleToLocation(_lastServerPosition);
			else
				player.sendPacket(new ValidateLocation(player));
		}
		else if(_diff > player.getMoveSpeed() * 2 && !player.isFalling())
		{
			//player.sendMessage("diff2: " + String.format("%.2f", _diff));
			if(!player.isFloating())
				player.sendPacket(new ValidateLocation(player));
		}
		//else if(player.isFloating())
		//	player.sendMessage("vd: " + String.format("%.2f ss: %.2f", _diff, player.getSwimSpeed()));

		if(player.getPet() != null && !player.getPet().isInRange())
			player.getPet().stopMove();

		//if(GeoConfig.GEODATA_DEBUG && "Rage".equals(player.getName()))
		//	_logD.info("ValidatePos: " + GameTimeController.getGameTicks() + " follow diff: " + String.format("%.2f", _diff) + " cDist: " + String.format("%.2f", _loc.getDistance(player.getXTo(), player.getYTo())));

		player.setLastClientPosition(_loc.setH(player.getHeading()));
		player.setLastServerPosition(player.getLoc());
	}

	private void correctPosition(L2Player player)
	{
		if(_lastServerPosition.getX() != 0 && _lastServerPosition.getY() != 0 && _lastServerPosition.getZ() != 0)
		{
			if(GeoEngine.getNSWE(_lastServerPosition.getX(), _lastServerPosition.getY(), _lastServerPosition.getZ(), player.getReflection()) == 15)
			{
				player.setXYZ(_lastServerPosition.getX(), _lastServerPosition.getY(), GeoEngine.getHeight(_lastServerPosition, player.getReflection()), false);
				player.stopMove();
				player.sendPacket(new ValidateLocation(player));
			}
			else
				player.teleToClosestTown();
		}
		else if(_lastClientPosition.getX() != 0 && _lastClientPosition.getY() != 0 && _lastClientPosition.getZ() != 0)
		{
			if(GeoEngine.getNSWE(_lastClientPosition.getX(), _lastClientPosition.getY(), _lastClientPosition.getZ(), player.getReflection()) == 15)
			{
				player.setXYZ(_lastClientPosition.getX(), _lastClientPosition.getY(), GeoEngine.getHeight(_lastClientPosition, player.getReflection()), false);
				player.stopMove();
				player.sendPacket(new ValidateLocation(player));
			}
			else
				player.teleToClosestTown();
		}
		else
			player.teleToClosestTown();
	}
}