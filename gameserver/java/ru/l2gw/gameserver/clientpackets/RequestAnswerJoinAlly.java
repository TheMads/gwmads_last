package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Alliance;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestAnswerJoinAlly extends L2GameClientPacket
{
	// format: cd
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
		if(player != null)
		{
			L2Player requestor = player.getTransactionRequester();

			player.setTransactionRequester(null);

			if(requestor == null)
				return;

			requestor.setTransactionRequester(null);

			if(requestor.getAllyId() <= 0)
				return;

			if(player.getTransactionType() != TransactionType.ALLY || player.getTransactionType() != requestor.getTransactionType())
				return;

			if(_response == 1)
			{
				L2Clan clan = player.getClan();
				L2Alliance ally = requestor.getAlliance();
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ACCEPTED_THE_ALLIANCE));
				clan.setAllyId(requestor.getAllyId());
				clan.updateClanInDB();
				ally.addAllyMember(clan, true);
				PledgeShowInfoUpdate pi = new PledgeShowInfoUpdate(player.getClan());
				for(L2Player member : clan.getOnlineMembers(""))
				{
					member.sendPacket(pi);
					member.broadcastUserInfo();
				}
			}
			else
				requestor.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_INVITE_A_CLAN_INTO_THE_ALLIANCE));
		}
	}
}
