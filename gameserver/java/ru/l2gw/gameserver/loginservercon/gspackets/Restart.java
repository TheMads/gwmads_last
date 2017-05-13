package ru.l2gw.gameserver.loginservercon.gspackets;

public class Restart extends GameServerBasePacket
{
	public Restart()
	{
		writeC(0x09);
	}
}