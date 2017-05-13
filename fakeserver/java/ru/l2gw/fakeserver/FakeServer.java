package ru.l2gw.fakeserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.MMOConnection;
import ru.l2gw.commons.network.SelectorConfig;
import ru.l2gw.commons.network.SelectorThread;
import ru.l2gw.commons.network.telnet.TelnetServer;
import ru.l2gw.commons.network.telnet.TelnetServerHandler;
import ru.l2gw.commons.versioning.Version;
import ru.l2gw.fakeserver.manager.ServerManager;
import ru.l2gw.fakeserver.network.FakeClient;
import ru.l2gw.fakeserver.network.PacketHandler;
import ru.l2gw.fakeserver.network.telnet.commands.ServersCommand;
import ru.l2gw.fakeserver.network.telnet.commands.ShutdownCommand;
import ru.l2gw.fakeserver.network.telnet.commands.StatusCommand;
import ru.l2gw.fakeserver.threading.ThreadPoolManager;

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * @author: rage
 * @date: 18.04.13 13:20
 */
public class FakeServer
{
	private static final Log _log = LogFactory.getLog(FakeServer.class.getName());
	private final SelectorThread<FakeClient> _selectorThread;
	public static FakeServer fakeServer;
	private static Version version;
	private static ServerManager serverManager;
	public static TelnetServer statusServer;

	public FakeServer() throws Exception
	{
		ThreadPoolManager.getInstance();

		PacketHandler gph = new PacketHandler();
		SelectorConfig<FakeClient> sc = new SelectorConfig<>(null, gph);
		sc.setMaxSendPerPass(30);
		sc.setSelectorSleepTime(1);
		_selectorThread = new SelectorThread<>(sc, null, gph, gph, gph, null);
		_selectorThread.setAntiFlood(true);
		_selectorThread.setAntiFloodSocketsConf(5, 5);
		_selectorThread.openServerSocket(Config.FAKESERVER_HOSTNAME.equals("*") ? null : InetAddress.getByName(Config.FAKESERVER_HOSTNAME), Config.PORT_GAME);
		_selectorThread.start();

		_log.info("FakeServer Started");

		serverManager = new ServerManager();
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(serverManager, 10000, Config.REQUEST_DELAY);
		_log.info("Start request task for: " + Config.REQUEST_DELAY + " msec.");

		if(Config.TELNET_ENABLED)
		{
			TelnetServerHandler telnetHandler = new TelnetServerHandler(Config.TELNET_PASSWORD);
			telnetHandler.addHandler(new ShutdownCommand());
			telnetHandler.addHandler(new ServersCommand());
			telnetHandler.addHandler(new StatusCommand());
			statusServer = new TelnetServer(Config.TELNET_HOST, Config.TELNET_PORT, telnetHandler);
			_log.info("Telnet server started: " + Config.TELNET_HOST + ":" + Config.TELNET_PORT);
		}
		else
			_log.info("Telnet server is currently disabled.");

	}

	public static void main(String[] args) throws Exception
	{
		new File("./log/").mkdir();

		version = new Version(FakeServer.class);
		_log.info("Gameserver revision: " + version.getRevisionNumber() + " build: " + version.getBuildDate() + " " + version.getBuildBy());
		Version commonVersion = new Version(MMOConnection.class);
		_log.info("Commons revision: " + commonVersion.getRevisionNumber() + " build: " + commonVersion.getBuildDate() + " " + commonVersion.getBuildBy());

		// Initialize config
		Config.load();
		checkFreePorts();
		fakeServer = new FakeServer();

	}

	public static void checkFreePorts()
	{
		try
		{
			ServerSocket ss;
			if(Config.FAKESERVER_HOSTNAME.equalsIgnoreCase("*"))
				ss = new ServerSocket(Config.PORT_GAME);
			else
				ss = new ServerSocket(Config.PORT_GAME, 50, InetAddress.getByName(Config.FAKESERVER_HOSTNAME));
			ss.close();
		}
		catch(Exception e)
		{
			_log.warn("\nPort " + Config.PORT_GAME + " is allready binded. Please free it and restart server.");
			System.exit(0);
		}
	}

	public static ServerManager getServerManager()
	{
		return serverManager;
	}
}
