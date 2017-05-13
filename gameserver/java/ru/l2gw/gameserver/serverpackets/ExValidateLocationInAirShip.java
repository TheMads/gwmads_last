package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

public class ExValidateLocationInAirShip extends L2GameServerPacket
{
	private int airShipId, objectId, heading;
	private Location loc;

	public ExValidateLocationInAirShip(L2Player player)
	{
		airShipId = player.getVehicle().getObjectId();
		objectId = player.getObjectId();
		loc = player.getLocInVehicle();
		heading = player.getHeading();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x70);
		writeD(objectId);
		writeD(airShipId);
		writeD(loc.getX());
		writeD(loc.getY());
		writeD(loc.getZ());
		writeD(heading);
	}
}
