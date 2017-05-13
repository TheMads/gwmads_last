package ru.l2gw.gameserver.loginservercon;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.network.GameClient;

/**
 * @Author: Death
 * @Date: 13/11/2007
 * @Time: 20:46:51
 */
public class KickPlayerInGameTask implements Runnable
{
	private final GameClient client;

	public KickPlayerInGameTask(GameClient client)
	{
		this.client = client;
	}

	public void run()
	{
		L2Player player = client.getPlayer();

		if(player != null)
			player.logout(false, false, true);
		else
		{
			client.sendPacket(Msg.ServerClose);
			client.closeNow(false);
		}
	}
}
