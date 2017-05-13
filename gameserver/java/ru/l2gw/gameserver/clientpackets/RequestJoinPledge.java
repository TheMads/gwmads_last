package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.AskJoinPledge;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestJoinPledge extends L2GameClientPacket
{
	//Format: cdd
	private int _target;
	private int _pledgeType;

	@Override
	public void readImpl()
	{
		_target = readD();
		_pledgeType = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Clan clan = player.getClan();

		if(clan == null || !clan.canInvite())
		{
			player.sendPacket(new SystemMessage(SystemMessage.AFTER_A_CLAN_MEMBER_IS_DISMISSED_FROM_A_CLAN_THE_CLAN_MUST_WAIT_AT_LEAST_A_DAY_BEFORE_ACCEPTING_A_NEW_MEMBER));
			return;
		}

		if(player.isTransactionInProgress())
		{
			player.sendPacket(Msg.WAITING_FOR_ANOTHER_REPLY);
			return;
		}

		if(_target == player.getObjectId())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_ASK_YOURSELF_TO_APPLY_TO_A_CLAN));
			return;
		}

		//is the player have privilege to invite players
		if((player.getClanPrivileges() & L2Clan.CP_CL_JOIN_CLAN) != L2Clan.CP_CL_JOIN_CLAN)
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS));
			return;
		}

		L2Object object = player.getVisibleObject(_target);
		if(object == null || !object.isPlayer())
			return;
		L2Player member = (L2Player) object;

		player.fireMethodInvoked(MethodCollection.onActionRequest, new Object[] { "clan", member });

		if(member.isInBlockList(player) || clan.getTerritoryId() > 0 && TerritoryWarManager.getWar().isInProgress())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_REFUSED_TO_JOIN_THE_CLAN).addString(player.getName()));
			return;
		}

		if(AdminTemplateManager.checkBoolean("noClan", player))
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_JOIN_THE_CLAN_BECAUSE_ONE_DAY_HAS_NOT_YET_PASSED_SINCE_HE_SHE_LEFT_ANOTHER_CLAN).addString(member.getName()));
			member.sendPacket(new SystemMessage(SystemMessage.FAILED_TO_JOIN_THE_CLAN));
			return;
		}
		if(AdminTemplateManager.checkBoolean("noClan", player))
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_JOIN_THE_CLAN_BECAUSE_ONE_DAY_HAS_NOT_YET_PASSED_SINCE_HE_SHE_LEFT_ANOTHER_CLAN).addString(member.getName()));
			member.sendPacket(new SystemMessage(SystemMessage.FAILED_TO_JOIN_THE_CLAN));
			return;
		}

		if(member.getClanId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_WORKING_WITH_ANOTHER_CLAN).addString(member.getName()));
			return;
		}

		if(member.isTransactionInProgress())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addString(member.getName()));
			return;
		}

		if(_pledgeType == -1 && (member.getLevel() > 40 || member.getClassId().getLevel() > 2))
		{
			player.sendPacket(new SystemMessage(SystemMessage.TO_JOIN_A_CLAN_ACADEMY_CHARACTERS_MUST_BE_LEVEL_40_OR_BELOW_NOT_BELONG_ANOTHER_CLAN_AND_NOT_YET_COMPLETED_THEIR_2ND_CLASS_TRANSFER));
			return;
		}

		if(clan.getSubPledgeMembersCount(_pledgeType) >= clan.getSubPledgeLimit(_pledgeType))
		{
			if(_pledgeType == 0)
				player.sendPacket(new SystemMessage(SystemMessage.S1_IS_FULL_AND_CANNOT_ACCEPT_ADDITIONAL_CLAN_MEMBERS_AT_THIS_TIME).addString(clan.getName()));
			else
				player.sendPacket(new SystemMessage(SystemMessage.THE_ACADEMY_ROYAL_GUARD_ORDER_OF_KNIGHTS_IS_FULL_AND_CANNOT_ACCEPT_NEW_MEMBERS_AT_THIS_TIME));
			return;
		}

		member.setTransactionRequester(player, System.currentTimeMillis() + 10000);
		member.setTransactionType(TransactionType.CLAN);
		member.setPledgeType(_pledgeType);
		player.setTransactionRequester(member, System.currentTimeMillis() + 10000);
		player.setTransactionType(TransactionType.CLAN);

		member.sendPacket(new AskJoinPledge(player.getObjectId(), player.getClan().getName()));
	}
}
