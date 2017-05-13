package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.serverpackets.ExAskJoinMPCC;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 * Format: (ch) S
 */
public class RequestExMPCCAskJoin extends L2GameClientPacket
{
	private String _name;
	public static final int STRATEGY_GUIDE_ID = 8871;
	public static final int CLAN_IMPERIUM_ID = 391;

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null || !player.isInParty())
			return;

		L2Player target = L2ObjectsStorage.getPlayer(_name);

		// Чар с таким имененм не найден в мире
		if(target == null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.THAT_PLAYER_IS_NOT_CURRENTLY_ONLINE));
			return;
		}

		// Сам себя нельзя
		if(player == target)
			return;

		if(target.isInBlockList(player))
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DECLINED_THE_CHANNEL_INVITATION).addString(target.getName()));
			return;
		}

		L2Party activeParty = player.getParty();

		if(!activeParty.isInCommandChannel())
		{
			if(player.getClanId() == 0 || !activeParty.isLeader(player) || player.getPledgeRank() < L2Clan.PledgeRank.BARON.ordinal())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_USE_THE_COMMAND_CHANNEL));
				return;
			}

			boolean haveSkill = false;
			// CC моно создать, если есть клановый скилл Clan Imperium
			for(L2Skill skill : player.getAllSkills())
				if(skill.getId() == CLAN_IMPERIUM_ID)
				{
					haveSkill = true;
					break;
				}

			boolean haveItem = false;
			// Ищем Strategy Guide в инвентаре
			if(player.getItemCountByItemId(STRATEGY_GUIDE_ID) > 0)
				haveItem = true;

			if(!haveSkill && !haveItem)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_USE_THE_COMMAND_CHANNEL));
				return;
			}
		}

		// Приглашать в СС может только лидер CC
		if(activeParty.isInCommandChannel() && activeParty.getCommandChannel().getChannelLeader() != player)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_INVITE_SOMEONE_TO_THE_COMMAND_CHANNEL));
			return;
		}

		// Нельзя приглашать безпартийных и не лидеров партий
		if(!target.isInParty() || !target.getParty().isLeader(target))
		{
			player.sendPacket(Msg.YOU_HAVE_INVITED_WRONG_TARGET);
			return;
		}

		if(target.getParty().isInCommandChannel())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_PARTY_IS_ALREADY_A_MEMBER_OF_THE_COMMAND_CHANNEL).addString(_name));
			return;
		}

		// Чувак уже отвечает на какое-то приглашение
		if(target.isTransactionInProgress())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER));
			return;
		}

		player.setTransactionType(TransactionType.CHANNEL);
		target.setTransactionRequester(player, System.currentTimeMillis() + 30000);
		target.setTransactionType(TransactionType.CHANNEL);
		target.sendPacket(new ExAskJoinMPCC(player.getName()));
		player.sendMessage("You invited " + target.getName() + " to your Command Channel.");
	}
}