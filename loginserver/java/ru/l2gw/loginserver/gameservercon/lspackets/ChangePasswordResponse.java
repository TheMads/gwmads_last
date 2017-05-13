package ru.l2gw.loginserver.gameservercon.lspackets;

/**
 * @Author: Death
 * @Date: 8/2/2007
 * @Time: 16:07:45
 */
public class ChangePasswordResponse extends ServerBasePacket
{

	public ChangePasswordResponse(String account, boolean hasChanged)
	{
		writeC(0x06);
		writeS(account);
		writeD(hasChanged ? 1 : 0);
	}
}
