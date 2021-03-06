package ru.l2gw.loginserver.gameservercon.gspackets;

import ru.l2gw.loginserver.Config;
import ru.l2gw.loginserver.L2LoginClient;
import ru.l2gw.loginserver.LoginController;
import ru.l2gw.loginserver.SessionKey;
import ru.l2gw.loginserver.gameservercon.AttGS;
import ru.l2gw.loginserver.gameservercon.lspackets.PlayerAuthResponse;

/**
 * @Author: Death
 * @Date: 15/11/2007
 * @Time: 12:30:08
 */
public class PlayerAuthRequest extends ClientBasePacket
{
	public PlayerAuthRequest(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String account = readS();
		int playOkId1 = readD();
		int playOkId2 = readD();
		int loginOkId1 = readD();
		int loginOkId2 = readD();

		L2LoginClient client = LoginController.getInstance().getAuthedClient(account);

		if(client == null)
		{
			System.out.println("Client is null for account " + account);
			sendPacket(new PlayerAuthResponse(account));
			return;
		}

		SessionKey key = client.getSessionKey();

		int lPlayOk1 = key.playOkID1;
		int lPlayOk2 = key.playOkID2;
		int lLoginOk1 = key.loginOkID1;
		int lLoginOk2 = key.loginOkID2;

		boolean isAuthedOnLs;
		if(Config.SHOW_LICENCE)
			isAuthedOnLs = playOkId1 == lPlayOk1 && playOkId2 == lPlayOk2 && loginOkId1 == lLoginOk1 && loginOkId2 == lLoginOk2;
		else
			isAuthedOnLs = playOkId1 == lPlayOk1 && playOkId2 == lPlayOk2;

		sendPacket(new PlayerAuthResponse(client, isAuthedOnLs));
	}
}
