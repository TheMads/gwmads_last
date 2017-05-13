package ru.l2gw.gameserver.loginservercon.gspackets;

/**
 * @author rage
 * @date 13.01.11 13:15
 */
public class SendFakePlayersCount extends GameServerBasePacket
{
	public SendFakePlayersCount(int count)
	{
		writeC(0x0d);
		writeD(count);
	}
}
