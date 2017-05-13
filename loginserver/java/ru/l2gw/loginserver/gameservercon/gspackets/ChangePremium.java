package ru.l2gw.loginserver.gameservercon.gspackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.loginserver.LoginController;
import ru.l2gw.loginserver.gameservercon.AttGS;

/**
 * @author rage
 * @date 20.05.2010 12:37:54
 */
public class ChangePremium extends ClientBasePacket
{
	private static final Log log = LogFactory.getLog(ChangePassword.class.getName());

	public ChangePremium(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		LoginController.getInstance().changePremium(readS(), readD());
	}
}
