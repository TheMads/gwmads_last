package ru.l2gw.loginserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.commons.network.utils.AdvIP;
import ru.l2gw.loginserver.Config;
import ru.l2gw.loginserver.GameServerTable;
import ru.l2gw.loginserver.L2LoginClient;
import ru.l2gw.loginserver.gameservercon.GameServerInfo;
import ru.l2gw.loginserver.gameservercon.gspackets.ServerStatus;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * ServerList
 * Format: cc [cddcchhcdc]
 *
 * c: server list size (number of servers)
 * c: last server
 * [ (repeat for each servers)
 * c: server id (ignored by client?)
 * d: server ip
 * d: server port
 * c: age limit (used by client?)
 * c: pvp or not (used by client?)
 * h: current number of players
 * h: max number of players
 * c: 0 if server is down
 * d: 2nd bit: clock
 *    3rd bit: wont dsiplay server name
 *    4th bit: test server (used by client?)
 * c: 0 if you dont want to display brackets in front of sever name
 * ]
 *
 * Server will be considered as Good when the number of  online players
 * is less than half the maximum. as Normal between half and 4/5
 * and Full when there's more than 4/5 of the maximum number of players
 */
public final class ServerList extends L2LoginServerPacket
{
	private List<ServerData> _servers;
	private int _lastServer;
	private L2LoginClient _client;

	public class ServerData
	{
		public final String ip;
		public final int port;
		public final boolean pvp;
		public final int currentPlayers;
		public final int maxPlayers;
		public final boolean testServer;
		public final boolean brackets;
		public final boolean clock;
		public final int status;
		public final int server_id;
		public final byte totalCharacters;
		public final byte deleteCharacters;

		ServerData(String pIp, int pPort, boolean pPvp, boolean pTestServer, int pCurrentPlayers, int pMaxPlayers, boolean pBrackets, boolean pClock, int pStatus, int pServer_id, byte total, byte del)
		{
			ip = pIp;
			port = pPort;
			pvp = pPvp;
			testServer = pTestServer;
			currentPlayers = pCurrentPlayers;
			maxPlayers = pMaxPlayers;
			brackets = pBrackets;
			clock = pClock;
			status = pStatus;
			server_id = pServer_id;
			totalCharacters = total;
			deleteCharacters = del;
		}
	}

	public ServerList(L2LoginClient client)
	{
		_client = client;
		_servers = new FastList<ServerData>();
		_lastServer = client.getLastServer();
		for(GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
		{
			String client_ip = _client.getIpAddress();
			if(client_ip == null || client_ip.equalsIgnoreCase("Null IP"))
				continue;

			String ipAddr = Config.isInternalIP(client_ip) || client.isUseInternalIp() ? gsi.getInternalIp() : gsi.getExternalIp();
			if(ipAddr == null || ipAddr.equalsIgnoreCase("Null IP") || (gsi.isTestServer() && client.getAccessLevel() < 100))
				continue;

			if(ipAddr.equalsIgnoreCase("*"))
				ipAddr = client.getConnection().getSocket().getLocalAddress().getHostAddress();
			if(gsi.getAdvIP() != null)
				for(AdvIP ip : gsi.getAdvIP())
					if(GameServerTable.getInstance().CheckSubNet(client_ip, ip))
					{
						ipAddr = ip.ipadress;
						break;
					}

			int count = gsi.getCurrentPlayerCount() + gsi.getFakePlayerCount();
			int max = count > gsi.getMaxPlayers() ? count + 100 : gsi.getMaxPlayers();
			byte[] chars = gsi.getAccountInfo(client.getAccount());
			_servers.add(new ServerData(ipAddr, gsi.getPort(), gsi.isPvp(), gsi.isTestServer(), count, max, gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId(), chars[0], chars[1]));
		}
	}

	@Override
	public void write()
	{
		writeC(0x04);
		writeC(_servers.size());
		writeC(_lastServer);
		for(ServerData server : _servers)
		{
			writeC(server.server_id); // server id

			try
			{
				InetAddress i4 = InetAddress.getByName(server.ip);
				byte[] raw = i4.getAddress();
				writeC(raw[0] & 0xff);
				writeC(raw[1] & 0xff);
				writeC(raw[2] & 0xff);
				writeC(raw[3] & 0xff);
			}
			catch(UnknownHostException e)
			{
				e.printStackTrace();
				writeC(127);
				writeC(0);
				writeC(0);
				writeC(1);
			}

			writeD(server.port);
			writeC(0x00); // age limit
			writeC(server.pvp ? 0x01 : 0x00);
			writeH(server.currentPlayers);
			writeH(server.maxPlayers);
			writeC(server.status == ServerStatus.STATUS_DOWN ? 0x00 : 0x01);
			int bits = 0;

			// 2  -- relax server (clock icon)
			// 16 -- character creation restricted
			// 32 -- event server (black icon)
			// 64 -- F icon

			if(server.testServer)
				bits |= 64; // empty ??
			if(server.clock)
				bits |= 0x02;
			writeD(bits);
			writeC(server.brackets ? 0x01 : 0x00);
		}

        writeH(0x00);  //-??
        writeC(_servers.size());
        for(ServerData server : _servers)
        {
			writeC(server.server_id);
            writeC(server.status == ServerStatus.STATUS_DOWN ? 0x00 : server.totalCharacters);  //  all chars in account
            writeC(server.status == ServerStatus.STATUS_DOWN ? 0x00 : server.deleteCharacters);  // characters in delete mode
        }
	}
}