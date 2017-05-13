package ru.l2gw.gameserver.loginservercon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.loginservercon.lspackets.*;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 22:41:04
 */
public class PacketHandler
{
	private static final Log log = LogFactory.getLog(PacketHandler.class.getName());

	public static LoginServerBasePacket handlePacket(byte[] data, AttLS loginserver)
	{
		if(LSConnection.DEBUG_GS_LS)
			log.info("GS Debug: Processing packet from LS");

		LoginServerBasePacket packet = null;

		/*
		try
		{
			data = loginserver.decrypt(data);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		*/

		int id = data[0] & 0xFF;

		switch(id)
		{
			case 1:
				packet = new LoginServerFail(data, loginserver);
				break;
			case 2:
				packet = new AuthResponse(data, loginserver);
				break;
			case 3:
				packet = new PlayerAuthResponse(data, loginserver);
				break;
			case 4:
				packet = new KickPlayer(data, loginserver);
				break;
			case 5:
				packet = new BanIPList(data, loginserver);
				break;
			case 6:
				packet = new ChangePasswordResponse(data, loginserver);
				break;
			case 7:
				packet = new IpAction(data, loginserver);
				break;
			case 0x20:
				packet = new ResponseUpdateSecondAuth(data, loginserver);
				break;
			default:
				log.warn("LSConnection: Recieved unknown packet: " + id + ". Terminating connection.");
				//LSConnection.getInstance().shutdown();
				LSConnection.getInstance().restart();

		}

		return packet;
	}
}
