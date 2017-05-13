package ru.l2gw.loginserver.gameservercon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.loginserver.gameservercon.gspackets.*;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 19:05:16
 */
public class PacketHandler
{
	private static Log log = LogFactory.getLog(PacketHandler.class.getName());

	public static ClientBasePacket handlePacket(byte[] data, AttGS gameserver)
	{
		ClientBasePacket packet = null;
		int packetType = data[0] & 0xff;

		if(!gameserver.isAuthed() && packetType > 1)
		{
			log.warn("Packet id[" + packetType + "] from not authed server.");
			return null;
		}

		switch(packetType)
		{
			case 0x01:
				new AuthRequest(data, gameserver).run();
				break;
			case 0x02:
				packet = new PlayerInGame(data, gameserver);
				break;
			case 0x03:
				packet = new PlayerLogout(data, gameserver);
				break;
			case 0x04:
				packet = new ChangeAccessLevel(data, gameserver);
				break;
			case 0x05:
				packet = new PlayerAuthRequest(data, gameserver);
				break;
			case 0x06:
				packet = new ServerStatus(data, gameserver);
				break;
			case 0x07:
				packet = new BanIP(data, gameserver);
				break;
			case 0x08:
				packet = new ChangePassword(data, gameserver);
				break;
			case 0x09:
				packet = new Restart(data, gameserver);
				break;
			case 0x0a:
				packet = new UnbanIP(data, gameserver);
				break;
			case 0x0b:
				packet = new LockAccountIP(data, gameserver);
				break;
			case 0x0c:
				packet = new ChangePremium(data, gameserver);
				break;
			case 0x0d:
				packet = new ReceiveFakePlayersCount(data, gameserver);
				break;
			case 0x0e:
				packet = new ReceiveBanLastIP(data, gameserver);
				break;
			case 0x0f:
				packet = new ReceiveAccountInfoList(data, gameserver);
				break;
			case 0x10:
				packet = new ReceiveAccountInfoUpdate(data, gameserver);
				break;
			case 0x20:
				packet = new RequestUpdateSecondAuth(data, gameserver);
				break;
			case 0x21:
				packet = new RequestUpdateSecondFail(data, gameserver);
				break;
			default:
				log.warn("Unknown packet from GS: " + packetType);

		}

		return packet;
	}
}