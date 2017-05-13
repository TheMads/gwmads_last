package ru.l2gw.loginserver.gameservercon.gspackets;

import ru.l2gw.loginserver.IpManager;
import ru.l2gw.loginserver.gameservercon.AttGS;
import ru.l2gw.loginserver.gameservercon.GSConnection;
import ru.l2gw.loginserver.gameservercon.lspackets.BanIPList;
import ru.l2gw.loginserver.gameservercon.lspackets.IpAction;

/**
 * @author rage
 * @date 25.02.11 15:35
 */
public class ReceiveBanLastIP extends ClientBasePacket
{

	public ReceiveBanLastIP(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String account = readS();
		String admin = readS();
		String comment = readS();
		String ip = IpManager.getInstance().banLastIp(account, admin, comment);
		if(ip != null && !ip.isEmpty())
		{
			GSConnection.getInstance().broadcastPacket(new BanIPList());
			GSConnection.getInstance().broadcastPacket(new IpAction(ip, true, admin));
		}
	}
}