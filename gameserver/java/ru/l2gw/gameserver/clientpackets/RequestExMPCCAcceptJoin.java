package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2CommandChannel;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestExMPCCAcceptJoin extends L2GameClientPacket
{
	private int _response;

	/**
	 * format: chdd
	 */
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

		if(player.getTransactionType() != TransactionType.CHANNEL || player.getTransactionType() != requestor.getTransactionType())
			return;

		if(!requestor.isInParty() || !player.isInParty() || player.getParty().isInCommandChannel())
		{
			requestor.sendPacket(new SystemMessage(SystemMessage.NO_USER_HAS_BEEN_INVITED_TO_THE_COMMAND_CHANNEL));
			return;
		}

		if(_response == 1 && player.isTeleporting())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_JOIN_A_COMMAND_CHANNEL_WHILE_TELEPORTING));
			requestor.sendPacket(new SystemMessage(SystemMessage.NO_USER_HAS_BEEN_INVITED_TO_THE_COMMAND_CHANNEL));
			return;
		}

		if(_response == 1)
		{
			// Создаем CC если его нет
			if(!requestor.getParty().isInCommandChannel())
			{
				if(requestor.getClanId() == 0 || !requestor.getParty().isLeader(requestor) || requestor.getPledgeRank() < L2Clan.PledgeRank.BARON.ordinal())
				{
					requestor.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_USE_THE_COMMAND_CHANNEL));
					return;
				}

				boolean haveSkill = false;
				// CC моно создать, если есть клановый скилл Clan Imperium
				for(L2Skill skill : requestor.getAllSkills())
					if(skill.getId() == RequestExMPCCAskJoin.CLAN_IMPERIUM_ID)
					{
						haveSkill = true;
						break;
					}

				boolean haveItem = false;
				// Ищем Strategy Guide в инвентаре
				if(requestor.getItemCountByItemId(RequestExMPCCAskJoin.STRATEGY_GUIDE_ID) > 0)
					haveItem = true;

				if(!haveSkill && !haveItem)
				{
					requestor.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_USE_THE_COMMAND_CHANNEL));
					return;
				}

				// Скила нету, придется расходовать предмет
				if(!haveSkill && haveItem)
					requestor.destroyItemByItemId("CommandChanel", RequestExMPCCAskJoin.STRATEGY_GUIDE_ID, 1, null, true);

				new L2CommandChannel(requestor); // Создаём Command Channel
				requestor.sendPacket(new SystemMessage(SystemMessage.THE_COMMAND_CHANNEL_HAS_BEEN_FORMED));
			}

			requestor.getParty().getCommandChannel().addParty(player.getParty());
		}
		else
			requestor.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DECLINED_THE_CHANNEL_INVITATION).addString(player.getName()));
	}
}