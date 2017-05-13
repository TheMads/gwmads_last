package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.serverpackets.ExShowAgitInfo;

public class RequestAllAgitInfo extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		getClient().getPlayer().sendPacket(new ExShowAgitInfo());
	}
}