package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.util.Map;

public class RequestFriendList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		player.sendPacket(Msg._FRIENDS_LIST_);
		Map<Integer, String> _list = player.getFriendList().getList();
		for(int objectId : _list.keySet())
		{
			L2Player friend = L2ObjectsStorage.getPlayer(objectId);
			if(friend != null && !friend.isInOfflineMode())
				player.sendPacket(new SystemMessage(SystemMessage.S1_CURRENTLY_ONLINE).addCharName(friend));
			else
				player.sendPacket(new SystemMessage(SystemMessage.S1_CURRENTLY_OFFLINE).addString(_list.get(objectId)));
		}
		player.sendPacket(Msg.__EQUALS__);
	}
}