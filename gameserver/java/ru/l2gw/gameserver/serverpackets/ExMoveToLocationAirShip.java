package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

public class ExMoveToLocationAirShip extends L2GameServerPacket
{
	private final int airShipId;
	private Location origin, destination;

	public ExMoveToLocationAirShip(L2Vehicle vehicle)
	{
		airShipId = vehicle.getObjectId();
		origin = vehicle.getLoc();
		destination = vehicle.getDestination();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x66);

		writeD(airShipId);
		writeD(destination.getX());
		writeD(destination.getY());
		writeD(destination.getZ());
		writeD(origin.getX());
		writeD(origin.getY());
		writeD(origin.getZ());
	}
}
