package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.serverpackets.ExShowCastleInfo;

public class RequestAllCastleInfo extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		getClient().getPlayer().sendPacket(new ExShowCastleInfo());
	}
}