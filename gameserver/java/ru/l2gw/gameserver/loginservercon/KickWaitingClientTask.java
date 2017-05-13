package ru.l2gw.gameserver.loginservercon;

import ru.l2gw.gameserver.network.GameClient;

/**
 * @Author: Death
 * @Date: 13/11/2007
 * @Time: 20:14:14
 */
public class KickWaitingClientTask implements Runnable
{
	private final GameClient client;

	public KickWaitingClientTask(GameClient client)
	{
		this.client = client;
	}

	public void run()
	{
		client.closeNow(false);
	}
}
