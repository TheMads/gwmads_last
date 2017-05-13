package ru.l2gw.loginserver.gameservercon.gspackets;

import ru.l2gw.loginserver.IpManager;
import ru.l2gw.loginserver.gameservercon.AttGS;
import ru.l2gw.loginserver.gameservercon.GSConnection;
import ru.l2gw.loginserver.gameservercon.lspackets.BanIPList;
import ru.l2gw.loginserver.gameservercon.lspackets.IpAction;

/**
 * @author -Wooden-
 *
 */
public class UnbanIP extends ClientBasePacket
{
	public UnbanIP(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String ip = readS();
		IpManager.getInstance().UnbanIp(ip);

		GSConnection.getInstance().broadcastPacket(new BanIPList());
		GSConnection.getInstance().broadcastPacket(new IpAction(ip, false, ""));
	}
}