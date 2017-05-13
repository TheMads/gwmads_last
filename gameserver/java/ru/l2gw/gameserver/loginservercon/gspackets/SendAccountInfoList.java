package ru.l2gw.gameserver.loginservercon.gspackets;

import javolution.util.FastMap;

/**
 * @author rage
 * @date 03.02.11 17:32
 */
public class SendAccountInfoList extends GameServerBasePacket
{
	public SendAccountInfoList(FastMap<String, byte[]> info)
	{
		writeC(0x0f);
		writeD(info.size());

		for(String account : info.keySet())
		{
			writeS(account);
			writeC(info.get(account)[0]);
			writeC(info.get(account)[1]);
		}
	}
}