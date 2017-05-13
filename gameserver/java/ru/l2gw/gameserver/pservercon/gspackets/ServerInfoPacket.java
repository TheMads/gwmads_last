package ru.l2gw.gameserver.pservercon.gspackets;

import ru.l2gw.gameserver.Config;

/**
 * @author: rage
 * @date: 16.10.11 0:24
 */
public class ServerInfoPacket extends GSBasePacket
{
	public ServerInfoPacket()
	{
		writeH(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(Config.REQUEST_ID);
	}
}
