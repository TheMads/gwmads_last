package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestExMPCCExit extends L2GameClientPacket
{
	private String _name;

	/**
	 * format: chS
	 */
	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || !player.isInParty() || !player.getParty().isInCommandChannel())
			return;

		L2Player target = L2ObjectsStorage.getPlayer(_name);

		// Чар с таким имененм не найден в мире
		if(target == null)
		{
			player.sendPacket(Msg.THAT_PLAYER_IS_NOT_CURRENTLY_ONLINE);
			return;
		}

		// Сам себя нельзя
		if(player == target)
			return;

		// Указанный чар не в пати, не в СС, в чужом СС
		if(!target.isInParty() || !target.getParty().isInCommandChannel() || player.getParty().getCommandChannel() != target.getParty().getCommandChannel())
		{
			player.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		// Это может делать только лидер СС
		if(player.getParty().getCommandChannel().getChannelLeader() != player)
		{
			player.sendPacket(Msg.ONLY_THE_CREATOR_OF_A_CHANNEL_CAN_ISSUE_A_GLOBAL_COMMAND);
			return;
		}

		target.getParty().getCommandChannel().getChannelLeader().sendPacket(new SystemMessage(SystemMessage.S1_PARTY_HAS_BEEN_DISMISSED_FROM_THE_COMMAND_CHANNEL).addString(target.getName()));
		target.getParty().getCommandChannel().removeParty(target.getParty());
		target.getParty().broadcastToPartyMembers(Msg.YOU_WERE_DISMISSED_FROM_THE_COMMAND_CHANNEL);
	}
}