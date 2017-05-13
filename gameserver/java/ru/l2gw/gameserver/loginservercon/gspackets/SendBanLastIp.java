package ru.l2gw.gameserver.loginservercon.gspackets;

/**
 * @author rage
 * @date 25.02.11 15:43
 */
public class SendBanLastIp extends GameServerBasePacket
{
	public SendBanLastIp(String account, String admin, String comment)
	{
		writeC(0x0e);
		writeS(account);
		writeS(admin);
		writeS(comment);
	}
}
