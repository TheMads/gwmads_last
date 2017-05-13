package ru.l2gw.loginserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.MMOConnection;
import ru.l2gw.commons.network.SelectorConfig;
import ru.l2gw.commons.network.SelectorThread;
import ru.l2gw.commons.network.telnet.TelnetServer;
import ru.l2gw.commons.network.telnet.TelnetServerHandler;
import ru.l2gw.commons.versioning.Version;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.ccpGuard.login.Antibrute;
import ru.l2gw.loginserver.gameservercon.GSConnection;
import ru.l2gw.loginserver.telnet.commands.*;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

public class L2LoginServer
{
	private static L2LoginServer _instance;
	private SelectorThread<L2LoginClient> _selectorThread;
	public static TelnetServer statusServer;
	public LoginController loginController;

	public static void main(String[] args) throws Exception
	{
		_instance = new L2LoginServer();
	}

	public static L2LoginServer getInstance()
	{
		return _instance;
	}

	public L2LoginServer() throws Exception
	{
		Log log = LogFactory.getLog(L2LoginServer.class.getName());
		new File("./log/").mkdir();

		Version version = new Version(L2LoginServer.class);
		log.info("Loginserver revision: " + version.getRevisionNumber() + " build: " + version.getBuildDate() + " " + version.getBuildBy());
		Version commonVersion = new Version(MMOConnection.class);
		log.info("Commons revision: " + commonVersion.getRevisionNumber() + " build: " + commonVersion.getBuildDate() + " " + commonVersion.getBuildBy());

		// Load Config
		Config.load();
		Antibrute.init();

		// Prepare Database
		Class.forName(Config.DATABASE_DRIVER).newInstance();
		DatabaseFactory.getInstance().getConnection().close();

		try
		{
			LoginController.load();
		}
		catch(GeneralSecurityException e)
		{
			log.fatal("FATAL: Failed initializing LoginController. Reason: " + e.getMessage());
			if(Config.LOGIN_DEBUG)
				e.printStackTrace();
			System.exit(1);
		}

		try
		{
			GameServerTable.load();
		}
		catch(GeneralSecurityException | SQLException e)
		{
			log.fatal("FATAL: Failed to load GameServerTable. Reason: " + e.getMessage());
			if(Config.LOGIN_DEBUG)
				e.printStackTrace();
			System.exit(1);
		}

		InetAddress ad = null;
		try
		{
			ad = InetAddress.getByName(Config.LOGIN_HOST);
		}
		catch(Exception e)
		{}

		L2LoginPacketHandler loginPacketHandler = new L2LoginPacketHandler();
		SelectorHelper sh = new SelectorHelper();
		SelectorConfig<L2LoginClient> sc = new SelectorConfig<>(null, sh);
		try
		{
			_selectorThread = new SelectorThread<>(sc, null, loginPacketHandler, sh, sh, sh);
			_selectorThread.setAcceptFilter(sh);
		}
		catch(IOException e)
		{
			log.fatal("FATAL: Failed to open Selector. Reason: " + e.getMessage());
			if(Config.LOGIN_DEBUG)
				e.printStackTrace();
			System.exit(1);
		}

		GSConnection gameServerListener = GSConnection.getInstance();
		gameServerListener.start();
		log.info("Listening for GameServers on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);

		if(Config.TELNET_ENABLED)
		{
			TelnetServerHandler telnetHandler = new TelnetServerHandler(Config.TELNET_PASSWORD);
			telnetHandler.addHandler(new StatusCommand());
			telnetHandler.addHandler(new UnblockIpCommand());
			telnetHandler.addHandler(new BanIpListCommand());
			telnetHandler.addHandler(new ShutdownCommand());
			telnetHandler.addHandler(new RestartCommand());
			telnetHandler.addHandler(new BanIpCommand());
			telnetHandler.addHandler(new UnbanIpCommand());
			telnetHandler.addHandler(new SetPassCommand());
			statusServer = new TelnetServer(Config.TELNET_HOST, Config.TELNET_PORT, telnetHandler);
		}
		else
			log.info("LoginServer Telnet server is currently disabled.");

		try
		{
			_selectorThread.setAntiFlood(Config.ANTIFLOOD_ENABLE);
			if(Config.ANTIFLOOD_ENABLE)
				_selectorThread.setAntiFloodSocketsConf(Config.MAX_UNHANDLED_SOCKETS_PER_IP, Config.UNHANDLED_SOCKET_MIN_TTL);
			_selectorThread.openServerSocket(ad, Config.PORT_LOGIN);
		}
		catch(IOException e)
		{
			log.fatal("FATAL: Failed to open server socket on " + ad + ":" + Config.PORT_LOGIN + ". Reason: " + e.getMessage());
			if(Config.LOGIN_DEBUG)
				e.printStackTrace();
			System.exit(1);
		}
		_selectorThread.start();
		log.info("Login Server ready on port " + Config.PORT_LOGIN);
		log.info(IpManager.getInstance().getBannedCount() + " banned IPs defined");

		Shutdown.getInstance().startShutdownH(Config.LRESTART_TIME, true);

		System.gc();
		// maxMemory is the upper limit the jvm can use, totalMemory the size of the current allocation pool, freeMemory the unused memory in the allocation pool
		long freeMem = (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1024 / 1024;
		long totalMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
		log.info("Free memory " + freeMem + " Mb of " + totalMem + " Mb");
	}

	public void shutdown(boolean restart)
	{
		Runtime.getRuntime().exit(restart ? 2 : 0);
	}

	public boolean unblockIp(String ipAddress)
	{
		return loginController.ipBlocked(ipAddress);
	}

	public boolean setPassword(String account, String password)
	{
		return loginController.setPassword(account, password);
	}
}