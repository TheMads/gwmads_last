package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;

import java.util.Map;

public class L2FriendList extends L2GameServerPacket
{
	private Map<Integer, String> _list;

	public L2FriendList(L2Player player)
	{
		_list = player.getFriendList().getList();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x75);
		writeD(_list.size());
		for(int objectId : _list.keySet())
		{
			writeD(0);
			writeS(_list.get(objectId)); //name
			L2Player friend = L2ObjectsStorage.getPlayer(objectId);
			writeD(friend != null && !friend.isInOfflineMode() ? 1 : 0); //online or offline
			writeD(objectId); //object_id
		}
	}
}