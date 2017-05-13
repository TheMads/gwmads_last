package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

public class MoveToLocationInVehicle extends L2GameServerPacket
{
	private Location _origin, _destination;
	private int _objId, _vehicleId;

	public MoveToLocationInVehicle(L2Player player, int vehicleId, Location destination, Location origin)
	{
		if(player == null)
			return;

		_objId = player.getObjectId();
		_vehicleId = vehicleId;
		_destination = destination;
		_origin = origin;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x7E);
		writeD(_objId);
		writeD(_vehicleId);
		writeD(_destination.getX());
		writeD(_destination.getY());
		writeD(_destination.getZ());
		writeD(_origin.getX());
		writeD(_origin.getY());
		writeD(_origin.getZ());
	}
}