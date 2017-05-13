package ru.l2gw.gameserver.loginservercon.gspackets;

/**
 * @Author: Death
 * @Date: 8/2/2007
 * @Time: 14:35:35
 */
public class ChangePassword extends GameServerBasePacket
{

	public ChangePassword(String account, String oldPass, String newPass)
	{
		writeC(0x08);
		writeS(account);
		writeS(oldPass);
		writeS(newPass);
	}
}
