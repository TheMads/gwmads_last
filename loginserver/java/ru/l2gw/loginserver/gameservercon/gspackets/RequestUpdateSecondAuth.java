package ru.l2gw.loginserver.gameservercon.gspackets;


import ru.l2gw.loginserver.LoginController;
import ru.l2gw.loginserver.gameservercon.AttGS;
import ru.l2gw.loginserver.gameservercon.lspackets.ResponseUpdateSecondAuth;

/**
 * @author: rage
 * @date: 25.04.13 0:33
 */
public class RequestUpdateSecondAuth extends ClientBasePacket
{
	public RequestUpdateSecondAuth(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String account = readS();
		String hash = readS();

		boolean res = LoginController.getInstance().updateSecondAuth(account, hash);
		getGameServer().sendPacket(new ResponseUpdateSecondAuth(account, res ? hash : ""));
	}
}
