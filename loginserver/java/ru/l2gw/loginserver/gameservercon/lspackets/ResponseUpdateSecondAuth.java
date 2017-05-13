package ru.l2gw.loginserver.gameservercon.lspackets;

/**
 * @author: rage
 * @date: 25.04.13 0:37
 */
public class ResponseUpdateSecondAuth extends ServerBasePacket
{
	public ResponseUpdateSecondAuth(String account, String hash)
	{
		writeC(0x20);
		writeS(account);
		writeS(hash);
	}
}
