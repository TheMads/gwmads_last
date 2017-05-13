package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ClanMember;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.PledgeShowMemberListUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestPledgeReorganizeMember extends L2GameClientPacket
{
	// format: (ch)dSdS
	int _replace;
	String _subjectName;
	int _targetUnit;
	String _replaceName;

	@Override
	public void readImpl()
	{
		_replace = readD();
		_subjectName = readS();
		_targetUnit = readD();
		if(_replace > 0)
			_replaceName = readS();
	}

	@Override
	public void runImpl()
	{
		//_log.warn("Received RequestPledgeReorganizeMember("+_arg1+","+_arg2+","+_arg3+","+_arg4+") from player "+getClient().getPlayer().getName());

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
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeReorganizeMember.ChangeAffiliations", player));
			player.sendActionFailed();
			return;
		}

		L2ClanMember subject = clan.getClanMember(_subjectName);
		if(subject == null)
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeReorganizeMember.NotInYourClan", player));
			player.sendActionFailed();
			return;
		}

		if(subject.getPledgeType() == _targetUnit)
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeReorganizeMember.AlreadyInThatCombatUnit", player));
			player.sendActionFailed();
			return;
		}

		if(_targetUnit != 0 && clan.getSubPledge(_targetUnit) == null)
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeReorganizeMember.NoSuchCombatUnit", player));
			player.sendActionFailed();
			return;
		}

		if(clan.isAcademy(_targetUnit))
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeReorganizeMember.AcademyViaInvitation", player));
			player.sendActionFailed();
			return;
		}
		/*
		 * unsure for next check, but anyway as workaround before academy refactoring
		 * (needs LvlJoinedAcademy to be put on L2ClanMember if so, to be able relocate from academy correctly)
		 */
		if(clan.isAcademy(subject.getPledgeType()))
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeReorganizeMember.CantMoveAcademyMember", player));
			player.sendActionFailed();
			return;
		}

		L2ClanMember replacement = null;

		if(_replace > 0)
		{
			replacement = clan.getClanMember(_replaceName);
			if(replacement == null)
			{
				player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeReorganizeMember.CharacterNotBelongClan", player));
				player.sendActionFailed();
				return;
			}
			if(replacement.getPledgeType() != _targetUnit)
			{
				player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeReorganizeMember.CharacterNotBelongCombatUnit", player));
				player.sendActionFailed();
				return;
			}
			if(replacement.isSubLeader() != 0)
			{
				player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeReorganizeMember.CharacterLeaderAnotherCombatUnit", player));
				player.sendActionFailed();
				return;
			}
		}
		else
		{
			if(clan.getSubPledgeMembersCount(_targetUnit) >= clan.getSubPledgeLimit(_targetUnit))
			{
				if(_targetUnit == 0)
					player.sendPacket(new SystemMessage(SystemMessage.S1_IS_FULL_AND_CANNOT_ACCEPT_ADDITIONAL_CLAN_MEMBERS_AT_THIS_TIME).addString(clan.getName()));
				else
					player.sendPacket(new SystemMessage(SystemMessage.THE_ACADEMY_ROYAL_GUARD_ORDER_OF_KNIGHTS_IS_FULL_AND_CANNOT_ACCEPT_NEW_MEMBERS_AT_THIS_TIME));
				player.sendActionFailed();
				return;
			}
			if(subject.isSubLeader() != 0)
			{
				player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestPledgeReorganizeMember.MemberLeaderAnotherUnit", player));
				player.sendActionFailed();
				return;
			}

		}

		if(replacement != null)
		{
			replacement.setPledgeType(subject.getPledgeType());
			if(replacement.getPowerGrade() > 5)
				replacement.setPowerGrade(clan.getAffiliationRank(replacement.getPledgeType()));
			clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(replacement));
			if(replacement.isOnline())
			{
				replacement.getPlayer().updatePledgeClass();
				replacement.getPlayer().broadcastUserInfo(true);
			}
		}

		subject.setPledgeType(_targetUnit);
		if(subject.getPowerGrade() > 5)
			subject.setPowerGrade(clan.getAffiliationRank(subject.getPledgeType()));
		clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(subject));
		if(subject.isOnline())
		{
			subject.getPlayer().updatePledgeClass();
			subject.getPlayer().broadcastUserInfo(true);
		}
	}
}
