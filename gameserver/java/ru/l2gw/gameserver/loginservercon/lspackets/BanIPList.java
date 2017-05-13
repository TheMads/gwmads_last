package ru.l2gw.gameserver.loginservercon.lspackets;

import javolution.util.FastList;
import ru.l2gw.commons.network.utils.BannedIp;
import ru.l2gw.gameserver.loginservercon.AttLS;

public class BanIPList extends LoginServerBasePacket
{
	FastList<BannedIp> baniplist = FastList.newInstance();

	public BanIPList(byte[] decrypt, AttLS loginServer)
	{
		super(decrypt, loginServer);
	}

	@Override
	public void read()
	{
		int size = readD();
		for(int i = 0; i < size; i++)
		{
			BannedIp ip = new BannedIp();
			ip.ip = readS();
			ip.admin = readS();
			baniplist.add(ip);
		}
	}
}