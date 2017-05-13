package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.L2Alliance;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;
import ru.l2gw.gameserver.tables.ClanTable;

public class RequestOustAlly extends L2GameClientPacket
{
	private String _clanName;

	@Override
	public void readImpl()
	{
		_clanName = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Clan leaderClan = player.getClan();
		if(leaderClan == null)
		{
			player.sendActionFailed();
			return;
		}
		L2Alliance alliance = leaderClan.getAlliance();
		if(alliance == null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS));
			return;
		}

		L2Clan clan;

		if(!player.isAllyLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.FEATURE_AVAILABLE_TO_ALLIANCE_LEADERS_ONLY));
			return;
		}

		if(_clanName == null)
			return;

		clan = ClanTable.getInstance().getClanByName(_clanName);

		if(clan != null)
		{
			if(!alliance.isMember(clan.getClanId()))
			{
				player.sendActionFailed();
				return;
			}

			if(alliance.getLeader().equals(clan))
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_WITHDRAW_FROM_THE_ALLIANCE));
				return;
			}

			clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.S1_S2).addString("Your clan has been expelled from " + alliance.getAllyName() + " alliance."));
			clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.A_CLAN_THAT_HAS_WITHDRAWN_OR_BEEN_EXPELLED_CANNOT_ENTER_INTO_AN_ALLIANCE_WITHIN_ONE_DAY_OF_WITHDRAWAL_OR_EXPULSION));
			clan.setAllyId(0);
			clan.setLeavedAlly();
			alliance.removeAllyMember(clan.getClanId());
			alliance.setExpelledMember();
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestOustAlly.ClanDismissed", player).addString(clan.getName()).addString(alliance.getAllyName()));

			PledgeShowInfoUpdate pi = new PledgeShowInfoUpdate(clan);
			for(L2Player member : clan.getOnlineMembers(""))
			{
				member.sendPacket(pi);
				member.broadcastUserInfo(true);
			}
		}
	}
}