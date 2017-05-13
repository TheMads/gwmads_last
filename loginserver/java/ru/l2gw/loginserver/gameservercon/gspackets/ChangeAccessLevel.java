package ru.l2gw.loginserver.gameservercon.gspackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.loginserver.LoginController;
import ru.l2gw.loginserver.gameservercon.AttGS;

/**
 * @author -Wooden-
 */
public class ChangeAccessLevel extends ClientBasePacket
{
	public static final Log log = LogFactory.getLog(ChangeAccessLevel.class.getName());

	public ChangeAccessLevel(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		int level = readD();
		String account = readS();
		String comments = readS();
		int banTime = readD();

		LoginController.getInstance().setAccountAccessLevel(account, level, comments, banTime);
		log.info("Changed " + account + " access level to " + level);
	}
}