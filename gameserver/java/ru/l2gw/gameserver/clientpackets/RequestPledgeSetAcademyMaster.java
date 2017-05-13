package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ClanMember;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.PledgeReceiveMemberInfo;
import ru.l2gw.gameserver.serverpackets.PledgeShowMemberListUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestPledgeSetAcademyMaster extends L2GameClientPacket
{
	// format: (ch)dSS
	private int _mode; // 1=set, 0=unset
	private String _sponsorName;
	private String _apprenticeName;

	@Override
	public void readImpl()
	{
		_mode = readD();
		_sponsorName = readS();
		_apprenticeName = readS();
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

		if((player.getClanPrivileges() & L2Clan.CP_CL_MASTER_RIGHTS) == L2Clan.CP_CL_MASTER_RIGHTS)
		{
			L2ClanMember sponsor = player.getClan().getClanMember(_sponsorName);
			L2ClanMember apprentice = player.getClan().getClanMember(_apprenticeName);
			if(sponsor != null && apprentice != null)
			{
				if(apprentice.getPledgeType() != L2Clan.SUBUNIT_ACADEMY || sponsor.getPledgeType() == L2Clan.SUBUNIT_ACADEMY)
					return; // hack?

				if(_mode == 1)
				{
					if(sponsor.hasApprentice())
					{
						player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestOustAlly.MemberAlreadyHasApprentice", player));
						return;
					}
					if(apprentice.hasSponsor())
					{
						player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestOustAlly.ApprenticeAlreadyHasSponsor", player));
						return;
					}
					sponsor.setApprentice(apprentice.getObjectId());
					clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(apprentice));
					clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.S2_HAS_BEEN_DESIGNATED_AS_THE_APPRENTICE_OF_CLAN_MEMBER_S1).addString(sponsor.getName()).addString(apprentice.getName()));
				}
				else
				{
					if(!sponsor.hasApprentice())
					{
						player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestOustAlly.MemberHasNoApprentice", player));
						return;
					}
					sponsor.setApprentice(0);
					clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(apprentice));
					clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.S2_CLAN_MEMBER_S1S_APPRENTICE_HAS_BEEN_REMOVED).addString(sponsor.getName()).addString(apprentice.getName()));
				}
				if(apprentice.isOnline())
					apprentice.getPlayer().broadcastUserInfo(true);
				player.sendPacket(new PledgeReceiveMemberInfo(sponsor));
			}
		}
		else
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestOustAlly.NoMasterRights", player));
	}
}
