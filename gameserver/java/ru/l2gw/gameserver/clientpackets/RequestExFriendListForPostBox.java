package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 03.02.11 13:27
 */
public class RequestExFriendListForPostBox extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		player.getFriendList().sendFriendListForPostBox();
	}
}
