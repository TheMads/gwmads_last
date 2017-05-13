package ru.l2gw.loginserver.gameservercon.gspackets;

import ru.l2gw.loginserver.gameservercon.AttGS;

/**
 * @author rage
 * @date 03.02.11 18:02
 */
public class ReceiveAccountInfoUpdate extends ClientBasePacket
{
	public ReceiveAccountInfoUpdate(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		getGameServer().getGameServerInfo().updateAccountInfo(readS(), (byte) readC(), (byte)readC());
	}
}
