package ru.l2gw.gameserver.loginservercon.lspackets;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.GameServer;
import ru.l2gw.gameserver.loginservercon.AttLS;
import ru.l2gw.gameserver.loginservercon.Attribute;
import ru.l2gw.gameserver.loginservercon.gspackets.PlayerInGame;
import ru.l2gw.gameserver.loginservercon.gspackets.ServerStatus;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;

public class AuthResponse extends LoginServerBasePacket
{
	private static final Log log = LogFactory.getLog(AuthResponse.class.getName());

	private int _serverId;
	private String _serverName;

	public AuthResponse(byte[] decrypt, AttLS loginServer)
	{
		super(decrypt, loginServer);
	}

	@Override
	public void read()
	{
		_serverId = readC();
		_serverName = readS();
		getLoginServer().setLicenseShown(readC() == 1);

		log.info("Registered on login as Server " + _serverId + " : " + _serverName);
		GameServer.setServerId(_serverId);

		FastList<Attribute> attributes = FastList.newInstance();

		attributes.add(new Attribute(Attribute.SERVER_LIST_SQUARE_BRACKET, Config.SERVER_LIST_BRACKET ? Attribute.ON : Attribute.OFF));
		attributes.add(new Attribute(Attribute.SERVER_LIST_CLOCK, Config.SERVER_LIST_CLOCK ? Attribute.ON : Attribute.OFF));
		attributes.add(new Attribute(Attribute.TEST_SERVER, Config.SERVER_LIST_TESTSERVER ? Attribute.ON : Attribute.OFF));
		attributes.add(new Attribute(Attribute.SERVER_LIST_STATUS, Config.SERVER_GMONLY ? Attribute.STATUS_GM_ONLY : Attribute.STATUS_AUTO));

		sendPacket(new ServerStatus(attributes));

		if(L2ObjectsStorage.getAllPlayers().size() > 0)
		{
			FastList<String> accountList = FastList.newInstance();
			for(L2Player player : L2ObjectsStorage.getAllPlayers())
			{
				if(player.isInOfflineMode())
					continue;
				accountList.add(player.getAccountName());
				getLoginServer().getCon().addAccountInGame(player.getNetConnection());
				if(accountList.size() >= 50)
				{
					PlayerInGame pig = new PlayerInGame(accountList);
					sendPacket(pig);
					accountList.clear();
				}
			}

			if(accountList.size() > 0)
			{
				PlayerInGame pig = new PlayerInGame(accountList);
				sendPacket(pig);
			}
		}
		/*
		FastMap<String, byte[]> list = GameServer.getAccountInfoList();

		if(list.size() > 0)
		{
			FastMap<String, byte[]> accountInfo = new FastMap<String, byte[]>();
			for(String account : list.keySet())
			{
				accountInfo.put(account, list.get(account));
				if(accountInfo.size() >= 50)
				{
					sendPacket(new SendAccountInfoList(accountInfo));
					accountInfo.clear();
				}
			}

			if(accountInfo.size() > 0)
				sendPacket(new SendAccountInfoList(accountInfo));
		}
		*/
	}

	/**
	 * @return Returns the serverId.
	 */
	public int getServerId()
	{
		return _serverId;
	}

	/**
	 * @return Returns the serverName.
	 */
	public String getServerName()
	{
		return _serverName;
	}
}
