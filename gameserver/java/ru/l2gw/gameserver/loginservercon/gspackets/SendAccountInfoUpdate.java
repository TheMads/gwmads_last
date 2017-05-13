package ru.l2gw.gameserver.loginservercon.gspackets;

/**
 * @author rage
 * @date 03.02.11 18:00
 */
public class SendAccountInfoUpdate extends GameServerBasePacket
{
	public SendAccountInfoUpdate(String account, byte total, byte deleted)
	{
		writeC(0x10);
		writeS(account);
		writeC(total);
		writeC(deleted);
	}
}