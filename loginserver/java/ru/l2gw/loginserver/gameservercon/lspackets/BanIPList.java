package ru.l2gw.loginserver.gameservercon.lspackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.network.utils.BannedIp;
import ru.l2gw.loginserver.IpManager;

/**
 * @author -Wooden-
 *
 */
public class BanIPList extends ServerBasePacket
{
	// ID 0x00
	// format
	// d proto rev
	// d key size
	// b key

	public BanIPList()
	{
		GArray<BannedIp> baniplist = IpManager.getInstance().getBanList();
		writeC(0x05);
		writeD(baniplist.size());
		for(BannedIp ip : baniplist)
		{
			writeS(ip.ip);
			writeS(ip.admin);
		}
	}
}
