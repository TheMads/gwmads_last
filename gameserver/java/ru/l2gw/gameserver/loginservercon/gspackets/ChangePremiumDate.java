package ru.l2gw.gameserver.loginservercon.gspackets;

/**
 * @author rage
 * @date 20.05.2010 12:35:17
 */
public class ChangePremiumDate extends GameServerBasePacket
{

	public ChangePremiumDate(String account, int premiumEndDate)
	{
		writeC(0x0C);
		writeS(account);
		writeD(premiumEndDate);
	}
}
