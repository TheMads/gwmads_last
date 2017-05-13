package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

public class RequestExJump extends L2GameClientPacket
{
	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getPlayer();
		if(activeChar == null)
			return;
		//sendPacket(new ExJumpToLocation(activeChar.getObjectId(), activeChar.getLoc(), activeChar.getLoc()));
		System.out.println(getType());
	}

	@Override
	public void readImpl()
	{}
}