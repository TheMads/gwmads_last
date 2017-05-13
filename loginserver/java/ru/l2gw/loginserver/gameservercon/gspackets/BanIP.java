package ru.l2gw.loginserver.gameservercon.gspackets;

import ru.l2gw.loginserver.IpManager;
import ru.l2gw.loginserver.gameservercon.AttGS;
import ru.l2gw.loginserver.gameservercon.GSConnection;
import ru.l2gw.loginserver.gameservercon.lspackets.BanIPList;
import ru.l2gw.loginserver.gameservercon.lspackets.IpAction;

public class BanIP extends ClientBasePacket
{

	public BanIP(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String ip = readS();
		String admin = readS();

		IpManager.getInstance().BanIp(ip, admin, 0, "");
		GSConnection.getInstance().broadcastPacket(new BanIPList());
		GSConnection.getInstance().broadcastPacket(new IpAction(ip, true, admin));
	}
}