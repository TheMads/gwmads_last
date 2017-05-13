package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 17.12.10 0:27
 */
public class AnswerPartyLootModification extends L2GameClientPacket
{
	public int _answer;
	
	@Override
	protected void readImpl()
	{
		_answer = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getPlayer();
		if (activeChar == null)
			return;

		L2Party party = activeChar.getParty();
		if(party != null)
			party.answerLootChangeRequest(activeChar, _answer == 1);
	}
}
