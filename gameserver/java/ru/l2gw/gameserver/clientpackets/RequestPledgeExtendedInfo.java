package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * Format: (c) S
 * S: pledge name?
 */
public class RequestPledgeExtendedInfo extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getPlayer();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestPledgeExtendedInfo");

		// TODO this
	}
}