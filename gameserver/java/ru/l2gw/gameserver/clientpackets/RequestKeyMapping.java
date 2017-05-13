package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExUISetting;

/**
 * @Author: Death
 * @Date: 17/11/2007
 * @Time: 22:05:25
 */
public class RequestKeyMapping extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player != null)
			player.sendPacket(new ExUISetting(player.getKeyBindings()));
	}
}
