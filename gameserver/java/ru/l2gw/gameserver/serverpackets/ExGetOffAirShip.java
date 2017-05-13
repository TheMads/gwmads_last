package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

public class ExGetOffAirShip extends L2GameServerPacket
{
	private final int playerId, airShipId;
	private Location loc;

	public ExGetOffAirShip(L2Player player, L2Vehicle vehicle, Location loc)
	{
		playerId = player.getObjectId();
		airShipId = vehicle.getObjectId();
		this.loc = loc;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x65);

		writeD(playerId);
		writeD(airShipId);
		writeD(loc.getX());
		writeD(loc.getY());
		writeD(loc.getZ());
	}
}
