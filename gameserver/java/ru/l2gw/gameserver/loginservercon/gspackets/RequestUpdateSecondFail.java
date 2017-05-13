package ru.l2gw.gameserver.loginservercon.gspackets;

/**
 * @author: rage
 * @date: 25.04.13 0:50
 */
public class RequestUpdateSecondFail extends GameServerBasePacket
{
	public RequestUpdateSecondFail(String account, int count)
	{
		writeC(0x21);
		writeS(account);
		writeD(count);
	}
}
