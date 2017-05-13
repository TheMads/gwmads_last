package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 03.02.11 13:31
 */
public class FriendListForPostBox extends L2GameServerPacket
{
	private final GArray<StatsSet> _friends;

	public FriendListForPostBox(GArray<StatsSet> friends)
	{
		_friends = friends;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x58);
		writeD(_friends.size());
		for(StatsSet friend : _friends)
		{
			writeD(0x00); // Friend id (internal on retail)
			writeS(friend.getString("name"));
			writeD(friend.getInteger("online"));
			writeD(friend.getInteger("objectId"));
			writeD(friend.getInteger("classId"));
			writeD(friend.getInteger("level"));
		}
	}
}
