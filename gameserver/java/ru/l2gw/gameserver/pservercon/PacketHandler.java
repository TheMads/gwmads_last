package ru.l2gw.gameserver.pservercon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.pservercon.pspackets.*;

/**
 * @author: rage
 * @date: 15.10.11 23:54
 */
public class PacketHandler
{
	private static final Log _log = LogFactory.getLog("product");

	public static PSBasePacket handlePacket(byte[] data, PSClient client)
	{
		if(Config.PRODUCT_SERVER_DEBUG)
			_log.info("PSConnection: Processing packet from Premium Server");

		PSBasePacket packet = null;

		int id = data[0] & 0xFF;

		switch(id)
		{
			case 0x00:
				packet = new VersionInfoPacket(data, client);
				break;
			case 0x0C:
				packet = new ResponseBuyProductItem(data, client);
				break;
			case 0x17:
				packet = new ResponseDeleteItems(data, client);
				break;
			case 0x1A:
				packet = new ResponseGamePoint(data, client);
				break;
			default:
				_log.warn("PSConnection: Received unknown packet: " + id + ". Terminating connection.");
				//LSConnection.getInstance().shutdown();
				PSConnection.getInstance().restart();
		}

		return packet;
	}
}
