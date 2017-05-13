package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.L2Friend;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestFriendDel extends L2GameClientPacket
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
		TryFriendDelete(player, _name);
	}

	public static boolean TryFriendDelete(L2Player player, String delFriend)
	{
		if(player == null || delFriend == null || delFriend.isEmpty())
			return false;

		String[] name = { delFriend };
		int objectId = player.getFriendList().removeFriend(name);
		if(objectId > 0)
		{
			L2Player friend = L2ObjectsStorage.getPlayer(objectId);

			player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_REMOVED_FROM_YOUR_FRIEND_LIST).addString(name[0]));
			player.sendPacket(new L2Friend(delFriend, false, friend != null, objectId)); //Офф посылает 0xFB Friend, хотя тут нету разници что именно посылать

			if(friend != null)
			{
				friend.getFriendList().getList().remove(player.getObjectId());
				friend.sendPacket(new SystemMessage(SystemMessage.S1__HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST).addString(player.getName()));
				friend.sendPacket(new L2Friend(player, false)); //Офф посылает 0xFB Friend, хотя тут нету разници что именно посылать
			}
			return true;
		}

		player.sendPacket(new SystemMessage(SystemMessage.S1_IS_NOT_ON_YOUR_FRIEND_LIST).addString(delFriend));
		return false;
	}
}
