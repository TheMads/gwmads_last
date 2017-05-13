package ru.l2gw.gameserver.loginservercon.gspackets;

/**
 * @Author: SYS
 * @Date: 10/4/2008
 */
public class LockAccountIP extends GameServerBasePacket
{

	public LockAccountIP(String account, String IP)
	{
		writeC(0x0b);
		writeS(account);
		writeS(IP);
	}
}