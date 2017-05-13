package ru.l2gw.gameserver.loginservercon.lspackets;

import ru.l2gw.gameserver.loginservercon.AttLS;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @Author: Death
 * @Date: 14/11/2007
 * @Time: 22:29:47
 */
public class IpAction extends LoginServerBasePacket
{
	public IpAction(byte[] decrypt, AttLS loginServer)
	{
		super(decrypt, loginServer);
	}

	@Override
	public void read()
	{
		String ip = readS();
		boolean isBan = readC() == 1;

		String gm = null;
		if(isBan)
			gm = readS();

		String message = isBan ? "IP: " + ip + " has been banned by " + gm : "IP: " + ip + " has been unbanned";
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			if(player.isGM())
				player.sendMessage(message);
	}
}
