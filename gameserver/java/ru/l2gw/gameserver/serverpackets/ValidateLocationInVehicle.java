package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

public class ValidateLocationInVehicle extends L2GameServerPacket
{
	private int _charObjId, _boatObjId;
	private Location _loc;

	/**
	 * 0x73 ValidateLocationInVehicle         hdd
	 */
	public ValidateLocationInVehicle(L2Player cha)
	{
		_charObjId = cha.getObjectId();
		_loc = cha.getLocInVehicle();
		_boatObjId = cha.getVehicle().getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x80);
		writeD(_charObjId);
		writeD(_boatObjId);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(_loc.getHeading());
	}
}