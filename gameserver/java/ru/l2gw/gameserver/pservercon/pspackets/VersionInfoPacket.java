package ru.l2gw.gameserver.pservercon.pspackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.pservercon.PSClient;
import ru.l2gw.gameserver.pservercon.PSConnection;

/**
 * @author: rage
 * @date: 16.10.11 0:28
 */
public class VersionInfoPacket extends PSBasePacket
{
	private static final Log _logStd = LogFactory.getLog(PSConnection.class);

	public VersionInfoPacket(byte[] data, PSClient client)
	{
		super(data, client);
	}

	@Override
	public void read()
	{
		int version = readD();
		if(version != Config.PRODUCT_SERVER_PROTOCOL)
		{
			_logStd.info("GameServer: Premium Server protocol version: " + version + " unsupported. Disconnect.");
			PSConnection.getInstance().shutdown();
		}
		else
		{
			_logStd.info("GameServer: Connected to PremiumServer " + Config.PRODUCT_SERVER_HOST + ":" + Config.PRODUCT_SERVER_PORT);
		}
	}
}