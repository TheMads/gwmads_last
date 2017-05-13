package ru.l2gw.gameserver.loginservercon.lspackets;

import ru.l2gw.gameserver.loginservercon.AttLS;

public class KickPlayer extends LoginServerBasePacket
{
	public KickPlayer(byte[] decrypt, AttLS loginserver)
	{
		super(decrypt, loginserver);
	}

	@Override
	public void read()
	{
		getLoginServer().getCon().kickAccountInGame(readS());
	}
}