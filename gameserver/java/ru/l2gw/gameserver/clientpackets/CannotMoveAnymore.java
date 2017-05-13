package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.StopMove;
import ru.l2gw.util.Location;

public class CannotMoveAnymore extends L2GameClientPacket
{
	private Location _loc = new Location(0, 0, 0);

	/**
	 * packet type id 0x47
	 *
	 * sample
	 *
	 * 36
	 * a8 4f 02 00 // x
	 * 17 85 01 00 // y
	 * a7 00 00 00 // z
	 * 98 90 00 00 // heading?
	 *
	 * format:		cdddd
	 * @param decrypt
	 */
	@Override
	public void readImpl()
	{
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
		if(player.inObserverMode())
			player.sendPacket(new StopMove(player.getObjectId(), _loc));
		else
			player.stopMove();
	}
}