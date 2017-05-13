package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

public class GetOffVehicle extends L2GameServerPacket
{
	private int charObjId, boatObjId;
	Location loc;

	public GetOffVehicle(L2Player player, L2Vehicle vehicle, Location loc)
	{
		this.loc = loc;
		charObjId = player.getObjectId();
		boatObjId = vehicle.getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x6F);
		writeD(charObjId);
		writeD(boatObjId);
		writeD(loc.getX());
		writeD(loc.getY());
		writeD(loc.getZ());
	}
}