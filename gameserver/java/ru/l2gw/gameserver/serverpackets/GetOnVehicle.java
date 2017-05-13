package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

public class GetOnVehicle extends L2GameServerPacket
{
	private int _x, _y, _z, char_obj_id, boat_obj_id;

	public GetOnVehicle(L2Player player, L2Vehicle vehicle, Location loc)
	{
		_x = loc.getX();
		_y = loc.getY();
		_z = loc.getZ();
		char_obj_id = player.getObjectId();
		boat_obj_id = vehicle.getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x6E);
		writeD(char_obj_id);
		writeD(boat_obj_id);
		writeD(_x);
		writeD(_y);
		writeD(_z);
	}
}
