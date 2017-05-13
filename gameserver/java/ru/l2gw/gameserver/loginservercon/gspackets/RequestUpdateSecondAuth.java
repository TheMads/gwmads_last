package ru.l2gw.gameserver.loginservercon.gspackets;

/**
 * @author: rage
 * @date: 25.04.13 0:10
 */
public class RequestUpdateSecondAuth extends GameServerBasePacket
{
	public RequestUpdateSecondAuth(String login, String hash)
	{
		writeC(0x20);
		writeS(login);
		writeS(hash);
	}
}
