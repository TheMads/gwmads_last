package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Alliance;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.serverpackets.AskJoinAlliance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestJoinAlly extends L2GameClientPacket
{
	// format: cd
	private int _id;

	@Override
	public void readImpl()
	{
		_id = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || player.getClanId() == 0 || player.getAllyId() <= 0)
			return;

		L2Alliance ally = player.getAlliance();
		if(ally.getMembersCount() >= Config.ALT_MAX_ALLY_SIZE)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_INVITE_A_CLAN_INTO_THE_ALLIANCE));
			return;
		}

		L2Player target = L2ObjectsStorage.getPlayer(_id);
		player.fireMethodInvoked(MethodCollection.onActionRequest, new Object[] { "clan", target });

		if(target == null)
		{
			player.sendPacket(Msg.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			return;
		}

		if(target.isInBlockList(player))
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_INVITE_A_CLAN_INTO_THE_ALLIANCE));
			return;
		}

		if(target.getClanId() <= 0)
		{
			player.sendActionFailed();
			return;
		}
		if(!player.isAllyLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.FEATURE_AVAILABLE_TO_ALLIANCE_LEADERS_ONLY));
			return;
		}
		L2Alliance tAlly = target.getAlliance();
		if(target.getAllyId() > 0 || ally.isMember(target.getClanId()))
		{
			//same or another alliance - no need to invite
			SystemMessage sm = new SystemMessage(SystemMessage.S1_CLAN_IS_ALREADY_A_MEMBER_OF_S2_ALLIANCE);
			sm.addString(target.getClan().getName());
			sm.addString(tAlly.getAllyName());
			player.sendPacket(sm);
			return;
		}
		if(!target.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_NOT_A_CLAN_LEADER).addString(target.getName()));
			return;
		}
		if(player.isAtWarWith(target.getClanId()) > 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_ALLY_WITH_A_CLAN_YOU_ARE_AT_BATTLE_WITH));
			return;
		}
		if(!target.getClan().canJoinAlly())
		{
			SystemMessage sm = new SystemMessage(SystemMessage.S1_CLAN_CANNOT_JOIN_THE_ALLIANCE_BECAUSE_ONE_DAY_HAS_NOT_YET_PASSED_SINCE_IT_LEFT_ANOTHER_ALLIANCE);
			sm.addString(target.getClan().getName());
			player.sendPacket(sm);
			return;
		}
		if(!ally.canInvite())
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestJoinAlly.InvitePenalty", player));
		if(player.isTransactionInProgress())
		{
			player.sendPacket(Msg.WAITING_FOR_ANOTHER_REPLY);
			return;
		}
		if(target.isTransactionInProgress())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addString(target.getName()));
			return;
		}
		target.setTransactionRequester(player, System.currentTimeMillis() + 10000);
		target.setTransactionType(TransactionType.ALLY);
		player.setTransactionRequester(target, System.currentTimeMillis() + 10000);
		player.setTransactionType(TransactionType.ALLY);
		//leader of alliance request an alliance.
		SystemMessage sm = new SystemMessage(SystemMessage.S2_THE_LEADER_OF_S1_HAS_REQUESTED_AN_ALLIANCE);
		sm.addString(ally.getAllyName());
		sm.addString(player.getName());
		target.sendPacket(sm);
		target.sendPacket(new AskJoinAlliance(player.getObjectId(), player.getName(), ally.getAllyName()));
	}
}
