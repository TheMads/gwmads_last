package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

/**
 * @author: rage
 * @date: 24.06.2010 21:56:08
 */
public class CannotMoveAnymoreInAirShip extends L2GameClientPacket
{
	private Location _loc = new Location(0, 0, 0);
	private int _boatid;

	@Override
	public void readImpl()
	{
		_boatid = readD();
		_loc.setX(readD());
		_loc.setY(readD());
		_loc.setZ(readD());
		_loc.setH(readD());

	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isInBoat() && player.getVehicle().getObjectId() == _boatid)
		{
			player.setLocInVehicle(_loc);
			player.stopMove();
		}
	}
}
