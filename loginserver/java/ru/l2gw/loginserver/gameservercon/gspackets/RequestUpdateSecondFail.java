package ru.l2gw.loginserver.gameservercon.gspackets;

import ru.l2gw.loginserver.LoginController;
import ru.l2gw.loginserver.gameservercon.AttGS;

/**
 * @author: rage
 * @date: 25.04.13 0:55
 */
public class RequestUpdateSecondFail extends ClientBasePacket
{
	public RequestUpdateSecondFail(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String account = readS();
		int failCount = readD();

		LoginController.getInstance().updateSecondFail(account, failCount);
	}
}
