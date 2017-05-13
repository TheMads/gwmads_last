package ru.l2gw.gameserver.loginservercon.gspackets;

public class UnbanIP extends GameServerBasePacket
{
	public UnbanIP(String ip)
	{
		writeC(0x0a);
		writeS(ip);
	}
}