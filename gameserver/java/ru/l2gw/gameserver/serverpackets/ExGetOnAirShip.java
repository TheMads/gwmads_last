package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

public class ExGetOnAirShip extends L2GameServerPacket
{
	private final int x, y, z, vehicleObjId, playerObjId;

	public ExGetOnAirShip(L2Player player, L2Vehicle vehicle, Location loc)
	{
		x = loc.getX();
		y = loc.getY();
		z = loc.getZ();
		vehicleObjId = vehicle.getObjectId();
		playerObjId = player.getObjectId();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x64);
		writeD(playerObjId);
		writeD(vehicleObjId);
		writeD(x);
		writeD(y);
		writeD(z);
	}
}
