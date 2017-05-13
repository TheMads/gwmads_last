package ru.l2gw.gameserver.loginservercon.gspackets;

import ru.l2gw.commons.network.utils.AdvIP;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.network.MMOSocket;

public class AuthRequest extends GameServerBasePacket
{
	public AuthRequest()
	{
		writeC(0x01);
		writeC(Config.REQUEST_ID);
		writeC(Config.ACCEPT_ALTERNATE_ID ? 0x01 : 0x00);
		writeC(0x00);
		writeS(MMOSocket.getInstance(0) == null ? Config.EXTERNAL_HOSTNAME : MMOSocket.getInstance(0));
		writeS(MMOSocket.getInstance(1) == null ? Config.INTERNAL_HOSTNAME : MMOSocket.getInstance(1));
		writeH(Config.PORT_GAME);
		writeD(Config.MAXIMUM_ONLINE_USERS);
		byte[] data = Config.HEX_ID;
		if(data == null)
			writeD(0);
		else
		{
			writeD(Config.HEX_ID.length);
			writeB(Config.HEX_ID);
		}
		writeD(Config.GAMEIPS.size());
		for(AdvIP ip : Config.GAMEIPS)
		{
			writeS(ip.ipadress);
			writeS(ip.ipmask);
			writeS(ip.bitmask);
		}
	}
}
