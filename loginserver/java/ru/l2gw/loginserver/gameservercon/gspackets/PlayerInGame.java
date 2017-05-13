package ru.l2gw.loginserver.gameservercon.gspackets;

import ru.l2gw.loginserver.gameservercon.AttGS;

/**
 * @author -Wooden-
 *
 */
public class PlayerInGame extends ClientBasePacket
{
	public PlayerInGame(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		int size = readH();
		for(int i = 0; i < size; i++)
			getGameServer().addAccountInGameServer(readS());
	}
}