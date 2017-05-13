package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.serverpackets.FriendAddRequest;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestFriendInvite extends L2GameClientPacket
{
	// format: cS
	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		TryFriendInvite(player, _name);
	}

	public static boolean TryFriendInvite(L2Player player, String addFriend)
	{
		if(player == null || addFriend == null || addFriend.isEmpty())
			return false;

		if(player.isTransactionInProgress())
		{
			player.sendPacket(Msg.WAITING_FOR_ANOTHER_REPLY);
			return false;
		}

		if(player.getName().equalsIgnoreCase(addFriend))
		{
			player.sendPacket(Msg.YOU_CANNOT_ADD_YOURSELF_TO_YOUR_OWN_FRIEND_LIST);
			return false;
		}

		L2Player friendChar = L2ObjectsStorage.getPlayer(addFriend);
		if(friendChar == null || friendChar.isInOfflineMode())
		{
			player.sendPacket(Msg.THE_USER_WHO_REQUESTED_TO_BECOME_FRIENDS_IS_NOT_FOUND_IN_THE_GAME);
			return false;
		}

		if(friendChar.isBlockAll())
		{
			player.sendPacket(Msg.THE_PERSON_IS_IN_A_MESSAGE_REFUSAL_MODE);
			return false;
		}

		if(friendChar.isInBlockList(player))
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_INVITE_A_FRIEND));
			return false;
		}

		if(player.getFriendList().getList().containsKey(friendChar.getObjectId()))
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_ALREADY_ON_YOUR_FRIEND_LIST).addString(friendChar.getName()));
			return false;
		}

		if(Olympiad.isRegisteredInComp(player) || player.getOlympiadGameId() >= 0)
		{
			player.sendPacket(Msg.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS);
			return false;
		}

		if(friendChar.isTransactionInProgress())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER));
			return false;
		}

		friendChar.setTransactionRequester(player, System.currentTimeMillis() + 10000);
		friendChar.setTransactionType(TransactionType.FRIEND);
		player.setTransactionRequester(friendChar, System.currentTimeMillis() + 10000);
		player.setTransactionType(TransactionType.FRIEND);
		friendChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_REQUESTED_TO_BECOME_FRIENDS).addString(player.getName()));
		friendChar.sendPacket(new FriendAddRequest(player.getName()));

		return true;
	}
}
