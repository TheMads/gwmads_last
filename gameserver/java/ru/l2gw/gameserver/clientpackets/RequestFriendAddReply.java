package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.serverpackets.L2Friend;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestFriendAddReply extends L2GameClientPacket
{
	private int _response;

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

		if(player.getTransactionType() != TransactionType.FRIEND || player.getTransactionType() != requestor.getTransactionType())
			return;

		if(_response == 1)
		{
			requestor.getFriendList().addFriend(player);
			player.getFriendList().addFriend(requestor);
			requestor.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_SUCCEEDED_IN_INVITING_A_FRIEND));
			// Player added to your friendlist
			requestor.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_ADDED_TO_YOUR_FRIEND_LIST).addString(player.getName()));
			// has joined as friend.
			player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_JOINED_AS_A_FRIEND).addString(requestor.getName()));

			//Обновисть список друзей. Вообще-то здесь должен слатся пакет 0xfb Friend, а не 0xfa FriendList, но от греха подальше :)
			requestor.sendPacket(new L2Friend(player, true));
			player.sendPacket(new L2Friend(requestor, true));
		}
		else
			requestor.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_INVITE_A_FRIEND));
	}
}
