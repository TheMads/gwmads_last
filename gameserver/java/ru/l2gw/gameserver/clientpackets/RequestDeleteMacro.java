package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

public class RequestDeleteMacro extends L2GameClientPacket
{
	private int _id;

	/**
	 * packet type id 0xce
	 *
	 * sample
	 *
	 * ce
	 * d // macro id
	 *
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		_id = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		player.deleteMacro(_id);
	}
}