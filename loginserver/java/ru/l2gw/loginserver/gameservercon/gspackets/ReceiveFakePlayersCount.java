package ru.l2gw.loginserver.gameservercon.gspackets;

import ru.l2gw.loginserver.gameservercon.AttGS;

/**
 * @author rage
 * @date 13.01.11 13:12
 */
public class ReceiveFakePlayersCount extends ClientBasePacket
{
	public ReceiveFakePlayersCount(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		getGameServer().setFakePlayersCount(readD());
	}
}
