package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.StartRotating;

public class StartRotatingC extends L2GameClientPacket
{
	private int _degree;
	private int _side;

	/**
	 * packet type id 0x5b
	 *
	 * sample
	 *
	 * 5b
	 * fb 0f 00 00 // degree (goes from 0 to 65535)
	 * 01 00 00 00 // side (01 00 00 00 = right, ff ff ff ff = left)
	 *
	 * format:		cdd
	 */
	@Override
	public void readImpl()
	{
		_degree = readD();
		_side = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		player.setHeading(_degree);
		player.broadcastPacket(new StartRotating(player, _degree, _side, 0));
	}
}