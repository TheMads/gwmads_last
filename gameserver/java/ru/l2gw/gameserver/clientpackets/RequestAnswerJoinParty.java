package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.serverpackets.JoinParty;
import ru.l2gw.gameserver.serverpackets.PartyMemberPosition;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 *  sample
 *  2a
 *  01 00 00 00
 *
 *  format  cdd
 */
public class RequestAnswerJoinParty extends L2GameClientPacket
{
	//Format: cd
	private int _response;

	@Override
	public void readImpl()
	{
		if(_buf.hasRemaining())
			_response = readD();
		else
			_response = 0;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Player requestor = player.getTransactionRequester();

		player.setTransactionRequester(null);

		if(requestor == null)
			return;

		requestor.setTransactionRequester(null);

		if(requestor.getParty() == null)
			return;

		if(player.getTransactionType() != TransactionType.PARTY || player.getTransactionType() != requestor.getTransactionType())
			return;

		if(requestor.isCursedWeaponEquipped() || player.isCursedWeaponEquipped())
		{
			return;
		}

		if(_response < 0 || player.getSessionVar("event_team_pvp") != null && player.getTeam() != requestor.getTeam())
		{
			requestor.sendPacket(new SystemMessage(SystemMessage.C1_IS_SET_TO_REFUSE_PARTY_REQUESTS_AND_CANNOT_RECEIVE_A_PARTY_REQUEST).addCharName(player));
			return;
		}

		requestor.sendPacket(new JoinParty(_response));

		if(_response == 1)
		{
			if(requestor.getParty().getMemberCount() >= 9)
			{
				player.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
				requestor.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
				return;
			}

			player.joinParty(requestor.getParty());
			// force update party position
			player.getParty().broadcastToPartyMembers(player, new PartyMemberPosition(player));
		}
		else
		{
			//activate garbage collection if there are no other members in party (happens when we were creating new one)
			if(requestor.getParty() != null && requestor.getParty().getMemberCount() == 1)
				requestor.setParty(null);
		}
	}
}
