package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

public class VehicleDeparture extends L2GameServerPacket
{
	private int _moveSpeed, _rotationSpeed;
	private int _boatObjId;
	private Location _loc;

	public VehicleDeparture(L2Vehicle vehicle)
	{
		_boatObjId = vehicle.getObjectId();
		_moveSpeed = (int) vehicle.getMoveSpeed();
		_rotationSpeed = vehicle.getRotationSpeed();
		_loc = vehicle.getDestination();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x6c);
		writeD(_boatObjId);
		writeD(_moveSpeed);
		writeD(_rotationSpeed);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
	}
}