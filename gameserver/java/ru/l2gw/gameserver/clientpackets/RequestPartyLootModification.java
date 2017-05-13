package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 17.12.10 0:25
 */
public class RequestPartyLootModification extends L2GameClientPacket
{
	private byte _mode;
	
	@Override
	protected void readImpl()
	{
		_mode = (byte) readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getPlayer();
		if(activeChar == null)
			return;

		if(_mode < 0 || _mode > L2Party.ITEM_ORDER_SPOIL)
			return;

		L2Party party = activeChar.getParty();
		if(party == null || _mode == party.getLootDistribution() || party.getPartyLeader() != activeChar)
			return;

		party.requestLootChange(_mode);
	}
}
