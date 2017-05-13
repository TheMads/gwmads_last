package ru.l2gw.gameserver.pservercon.gspackets;

import ru.l2gw.gameserver.Config;

/**
 * @author: rage
 * @date: 16.10.11 1:20
 */
public class RequestGamePoints extends GSBasePacket
{
	public RequestGamePoints(int jobId, int accountId)
	{
		writeH(0x1A);
		writeD(jobId);
		writeD(accountId);
		writeD(Config.REQUEST_ID);
	}
}