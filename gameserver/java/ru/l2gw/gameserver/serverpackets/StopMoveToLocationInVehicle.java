package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

public class StopMoveToLocationInVehicle extends L2GameServerPacket
{
	private int _boatid, _charObjid, _heading;
	private Location _loc;

	/**
	 * @param player
	 */
	public StopMoveToLocationInVehicle(L2Player player, L2Vehicle vehicle)
	{
		_boatid = vehicle.getObjectId();
		_charObjid = player.getObjectId();
		_loc = Util.convertWorldCoordToVehicle(vehicle.getLoc(), player.getLoc(), false);
		_heading = player.getHeading();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x7F);
		writeD(_charObjid);
		writeD(_boatid);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(_heading);
	}
}