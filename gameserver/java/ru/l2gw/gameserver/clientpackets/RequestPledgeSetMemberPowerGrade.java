package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ClanMember;
import ru.l2gw.gameserver.model.L2Player;

public class RequestPledgeSetMemberPowerGrade extends L2GameClientPacket
{
	// format: (ch)Sd
	private int _powerGrade;
	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
		_powerGrade = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Clan clan = player.getClan();
		if(clan == null)
			return;

		if((player.getClanPrivileges() & L2Clan.CP_CL_MANAGE_RANKS) == L2Clan.CP_CL_MANAGE_RANKS)
		{
			L2ClanMember member = player.getClan().getClanMember(_name);
			if(member != null)
			{
				if(clan.isAcademy(member.getPledgeType()))
				{
					player.sendMessage("You cannot change academy member grade");
					return;
				}
				if(_powerGrade > 5 && clan.getAffiliationRank(member.getPledgeType()) != _powerGrade)
					member.setPowerGrade(clan.getAffiliationRank(member.getPledgeType()));
				else
					member.setPowerGrade(_powerGrade);
				if(member.isOnline())
					member.getPlayer().sendUserInfo(false);
			}
			else
				player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeSetMemberPowerGrade.NotBelongClan", player));
		}
		else
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeSetMemberPowerGrade.HaveNotAuthority", player));
	}
}
