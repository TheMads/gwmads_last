package ru.l2gw.loginserver.gameservercon.gspackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.network.utils.AdvIP;
import ru.l2gw.loginserver.Config;
import ru.l2gw.loginserver.GameServerTable;
import ru.l2gw.loginserver.gameservercon.AttGS;
import ru.l2gw.loginserver.gameservercon.GameServerInfo;
import ru.l2gw.loginserver.gameservercon.lspackets.AuthResponse;
import ru.l2gw.loginserver.gameservercon.lspackets.BanIPList;
import ru.l2gw.loginserver.gameservercon.lspackets.LoginServerFail;

import java.util.Arrays;

/**
 * Format: cccddbd(sss)
 * c desired ID
 * c accept alternative ID
 * c reserve Host
 * s ExternalHostName
 * s InetranlHostName
 * d max players
 * d hexid size
 * b hexid
 * d size of AdvIP
 * (sss) Ip, IpMask, BitMask
 *
 * @author -Wooden-
 *
 */
public class AuthRequest extends ClientBasePacket
{
	protected static Log log = LogFactory.getLog(AuthRequest.class.getName());

	public AuthRequest(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		int requestId = readC();
		boolean acceptAlternateID = readC() == 1;
		boolean reserveHostOnLogin = readC() == 1; //FIXME: оно всегда false
		String externalIp = readS();
		String internalIp = readS();
		int port = readH();
		int maxOnline = readD();
		int hexIdLenth = readD();
		byte[] hexId = readB(hexIdLenth);
		int advIpsSize = readD();

		GArray<AdvIP> advIpList = new GArray<>(advIpsSize);

		for(int i = 0; i < advIpsSize; i++)
		{
			AdvIP ip = new AdvIP();
			ip.ipadress = readS();
			ip.ipmask = readS();
			ip.bitmask = readS();
			advIpList.add(ip);
		}

		log.info("Trying to register server: " + requestId + ", " + getGameServer().getConnectionIpAddress());

		GameServerTable gameServerTable = GameServerTable.getInstance();

		GameServerInfo gsi = gameServerTable.getRegisteredGameServerById(requestId);
		// is there a gameserver registered with this id?
		if(gsi != null)
		{
			// does the hex id match?
			if(Arrays.equals(gsi.getHexId(), hexId))
				// check to see if this GS is already connected
				synchronized (gsi)
				{
					if(gsi.isAuthed())
						sendPacket(new LoginServerFail(LoginServerFail.REASON_ALREADY_LOGGED8IN));
					else
					{
						getGameServer().setGameServerInfo(gsi);
						gsi.setGameServer(getGameServer());
						gsi.setPort(port);
						gsi.setGameHosts(externalIp, internalIp, advIpList);
						gsi.setMaxPlayers(maxOnline);
						gsi.setAuthed(true);
					}
				}
			else // there is already a server registered with the desired id and different hex id
			// try to register this one with an alternative id
			if(Config.ACCEPT_NEW_GAMESERVER && acceptAlternateID)
			{
				gsi = new GameServerInfo(requestId, hexId, getGameServer());
				if(gameServerTable.registerWithFirstAvailableId(gsi))
				{
					getGameServer().setGameServerInfo(gsi);
					gsi.setGameServer(getGameServer());
					gsi.setPort(port);
					gsi.setGameHosts(externalIp, internalIp, advIpList);
					gsi.setMaxPlayers(maxOnline);
					gsi.setAuthed(true);
					if(reserveHostOnLogin)
						gameServerTable.registerServerOnDB(gsi);
				}
				else
					sendPacket(new LoginServerFail(LoginServerFail.REASON_NO_FREE_ID));
			}
			else
				// server id is already taken, and we cant get a new one for you
				sendPacket(new LoginServerFail(LoginServerFail.REASON_WRONG_HEXID));
		}
		else if(Config.ACCEPT_NEW_GAMESERVER)
		{
			gsi = new GameServerInfo(requestId, hexId, getGameServer());
			if(gameServerTable.register(requestId, gsi))
			{
				getGameServer().setGameServerInfo(gsi);
				gsi.setGameServer(getGameServer());
				gsi.setPort(port);
				gsi.setGameHosts(externalIp, internalIp, advIpList);
				gsi.setMaxPlayers(maxOnline);
				gsi.setAuthed(true);
				if(reserveHostOnLogin)
					gameServerTable.registerServerOnDB(gsi);
			}
			else
				// some one took this ID meanwhile
				sendPacket(new LoginServerFail(LoginServerFail.REASON_ID_RESERVED));
		}
		else
			sendPacket(new LoginServerFail(LoginServerFail.REASON_WRONG_HEXID));

		if(gsi != null && gsi.isAuthed())
		{
			AuthResponse ar = new AuthResponse(gsi.getId());
			getGameServer().setAuthed(true);
			getGameServer().setServerId(gsi.getId());
			sendPacket(ar);
			sendPacket(new BanIPList());
			log.info("Server registration successful.");
		}
		else
			log.info("Server registration failed.");
	}
}
