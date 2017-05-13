package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Alliance;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;

/**
 * format: c
 */
public class RequestWithdrawAlly extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Clan clan = player.getClan();
		if(clan == null)
		{
			player.sendActionFailed();
			return;
		}

		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_MAY_APPLY_FOR_WITHDRAWAL_FROM_THE_ALLIANCE));
			return;
		}

		if(clan.getAlliance() == null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS));
			return;
		}

		if(clan.equals(clan.getAlliance().getLeader()))
		{
			player.sendPacket(new SystemMessage(SystemMessage.ALLIANCE_LEADERS_CANNOT_WITHDRAW));
			return;
		}

		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.YOU_HAVE_WITHDRAWN_FROM_THE_ALLIANCE));
		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.A_CLAN_THAT_HAS_WITHDRAWN_OR_BEEN_EXPELLED_CANNOT_ENTER_INTO_AN_ALLIANCE_WITHIN_ONE_DAY_OF_WITHDRAWAL_OR_EXPULSION));
		L2Alliance alliance = clan.getAlliance();
		clan.setAllyId(0);
		clan.setLeavedAlly();
		alliance.removeAllyMember(clan.getClanId());

		PledgeShowInfoUpdate pi = new PledgeShowInfoUpdate(clan);
		for(L2Player member : clan.getOnlineMembers(""))
		{
			member.sendPacket(pi);
			member.broadcastUserInfo();
		}
	}
}