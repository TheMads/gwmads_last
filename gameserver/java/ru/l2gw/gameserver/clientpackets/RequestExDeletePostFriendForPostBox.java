package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 03.02.11 13:47
 */
public class RequestExDeletePostFriendForPostBox extends L2GameClientPacket
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

		player.getContactList().removeContact(name);
	}

}
