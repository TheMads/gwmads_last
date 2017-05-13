package ru.l2gw.loginserver.gameservercon.gspackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.loginserver.Config;
import ru.l2gw.loginserver.GameServerTable;
import ru.l2gw.loginserver.LoginController;
import ru.l2gw.loginserver.gameservercon.AttGS;

/**
 * @author -Wooden-
 */
public class PlayerLogout extends ClientBasePacket
{
	public static final Log log = LogFactory.getLog(PlayerLogout.class.getName());

	public PlayerLogout(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String account = readS();
		getGameServer().removeAccountFromGameServer(account);
		LoginController.getInstance().removeAuthedLoginClient(account);

		if(Config.LOGIN_DEBUG)
			log.info("Player " + account + " logged out from gameserver [" + getGameServer().getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getGameServer().getServerId()));
	}
}