package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

public class Ride extends L2GameServerPacket
{
	private int id;
	private int rideClassID;
	private int mountType;
	private Location loc;

	public Ride(L2Player player)
	{
		if(player == null)
			return;

		id = player.getObjectId();
		mountType = player.getMountEngine().getMountType();
		rideClassID = player.getMountEngine().getMountNpcId() > 0 ? player.getMountEngine().getMountNpcId() + 1000000 : 0;
		loc = player.getLoc();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x8C);
		writeD(id);
		writeD(mountType == 0 ? 0 : 1);
		writeD(mountType);
		writeD(rideClassID);
		writeD(loc.getX());
		writeD(loc.getY());
		writeD(loc.getZ());
	}
}