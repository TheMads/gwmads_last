package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 17.12.10 0:21
 */
public class RequestResetNickname extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
		// nothing (trigger)
	}
	
	@Override
	protected void runImpl()
	{
		final L2Player activeChar = getClient().getPlayer();
		if (activeChar == null)
			return;
		
		activeChar.setTitleColor(0xFFFF77);
		activeChar.setTitle("");
		activeChar.broadcastUserInfo();
	}
}
