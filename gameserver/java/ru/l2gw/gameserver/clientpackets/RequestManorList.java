package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.serverpackets.ExSendManorList;

/**
 * Format: ch
 */
public class RequestManorList extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		getClient().sendPacket(new ExSendManorList());
	}
}