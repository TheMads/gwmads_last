package ru.l2gw.loginserver.gameservercon.lspackets;

/**
 * @Author: Death
 * @Date: 19/11/2007
 * @Time: 10:46:18
 */
public class IpAction extends ServerBasePacket
{
	public IpAction(String ip, boolean ban, String gm)
	{
		writeC(0x07);
		writeS(ip);
		writeC(ban ? 1 : 0);
		if(ban)
			writeS(gm);
	}
}
