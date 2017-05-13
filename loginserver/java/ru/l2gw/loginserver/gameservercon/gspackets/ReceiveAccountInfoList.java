package ru.l2gw.loginserver.gameservercon.gspackets;

import ru.l2gw.loginserver.gameservercon.AttGS;

/**
 * @author rage
 * @date 03.02.11 17:54
 */
public class ReceiveAccountInfoList extends ClientBasePacket
{
	public ReceiveAccountInfoList(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		int size = readD();
		for(int i = 0; i < size; i++)
			getGameServer().getGameServerInfo().updateAccountInfo(readS(), (byte) readC(), (byte)readC());
	}
}
