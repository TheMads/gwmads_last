package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

public class ExStopMoveInAirShip extends L2GameServerPacket
{
	private int charId;
	private int shipId;
	private int heading;
	private Location loc;

	public ExStopMoveInAirShip(L2Player player, L2Vehicle vehicle)
	{
		shipId = vehicle.getObjectId();
		charId = player.getObjectId();
		loc = Util.convertWorldCoordToVehicle(player.getVehicle().getLoc(), player.getLoc(), true);
		heading = player.getHeading();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x6F);
		writeD(charId);
		writeD(shipId);
		writeD(loc.getX());
		writeD(loc.getY());
		writeD(loc.getZ());
		writeD(heading);
	}
}
