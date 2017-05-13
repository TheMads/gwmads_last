package ru.l2gw.gameserver.serverpackets;

import java.util.Collection;

/**
 * @author rage
 * @date 03.02.11 13:25
 */
public class ExReceiveShowPostFriend extends L2GameServerPacket
{
	private Collection<String> _contactList;

	public ExReceiveShowPostFriend(Collection<String> contactList)
	{
		_contactList = contactList;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xD4);
		writeD(_contactList.size());
		for(String name : _contactList)
			writeS(name);
	}
}
