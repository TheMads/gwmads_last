package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PartyRoom;
import ru.l2gw.gameserver.serverpackets.ExAskJoinPartyRoom;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestAskJoinPartyRoom extends L2GameClientPacket
{
	private String name;

	@Override
	public void readImpl()
	{
		name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		L2Player target = L2ObjectsStorage.getPlayer(name);

		if(target == null || target.equals(player))
		{
			player.sendActionFailed();
			return;
		}

		if(player.getPartyRoom() <= 0)
		{
			player.sendActionFailed();
			return;
		}

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

		if(target.getPartyRoom() > 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_A_MEMBER_OF_ANOTHER_PARTY_AND_CANNOT_BE_INVITED).addString(target.getName()));
			return;
		}

		PartyRoom room = PartyRoomManager.getInstance().getRooms().get(player.getPartyRoom());
		if(room == null)
		{
			player.sendActionFailed();
			return;
		}

		if(room.getMembers().size() >= room.getMaxMembers())
		{
			player.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
			return;
		}

		if(!PartyRoomManager.getInstance().isLeader(player))
		{
			player.sendPacket(Msg.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
			return;
		}

		target.setTransactionRequester(player, System.currentTimeMillis() + 10000);
		target.setTransactionType(L2Player.TransactionType.PARTY_ROOM);
		player.setTransactionRequester(target, System.currentTimeMillis() + 10000);
		player.setTransactionType(L2Player.TransactionType.PARTY_ROOM);

		target.sendPacket(new ExAskJoinPartyRoom(player.getName()));
		player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_INVITED_YOU_TO_ENTER_THE_PARTY_ROOM).addString(target.getName()));
	}
}