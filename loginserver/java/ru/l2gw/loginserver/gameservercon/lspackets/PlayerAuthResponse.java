package ru.l2gw.loginserver.gameservercon.lspackets;

import ru.l2gw.loginserver.L2LoginClient;

/**
 * @Author: Death
 * @Date: 15/11/2007
 * @Time: 12:55:39
 */
public class PlayerAuthResponse extends ServerBasePacket
{
	public PlayerAuthResponse(L2LoginClient client, boolean authedOnLs)
	{
		writeC(3);
		writeS(client.getAccount());
		writeC(authedOnLs ? 1 : 0);
		writeD(client.getSessionKey().playOkID1);
		writeD(client.getSessionKey().playOkID2);
		writeD(client.getSessionKey().loginOkID1);
		writeD(client.getSessionKey().loginOkID2);
		writeD(client.getPremiumExpire());
		writeS(client.getAllowdIps());
		writeD(client.getAccountId());
		writeS(client.getSecondPass());
		writeD(client.getFailCount());
		writeC(client.isSecondUse() ? 0x01 : 0x00);
	}

	/**
	 * Если читер попытался зайти без LS, то передаем просто его имя.
	 * @param name имя читера
	 */
	public PlayerAuthResponse(String name)
	{
		writeC(3);
		writeS(name);
		writeC(0);
		writeD(0);
		writeD(0);
		writeD(0);
		writeD(0);
		writeD(0);
		writeS("");
		writeD(-1);
		writeS("");
		writeD(0x00);
		writeC(0x00);
	}
}
