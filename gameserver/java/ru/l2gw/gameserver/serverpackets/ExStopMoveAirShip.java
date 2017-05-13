package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;

public class ExStopMoveAirShip extends L2GameServerPacket
{
	private int x, y, z, h, objectId;

	public ExStopMoveAirShip(L2Vehicle vehicle)
	{
		objectId = vehicle.getObjectId();
		x = vehicle.getX();
		y = vehicle.getY();
		z = vehicle.getZ();
		h = vehicle.getHeading();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x67);
		writeD(objectId);
		writeD(x);
		writeD(y);
		writeD(z);
		writeD(h);
	}
}
