package ru.l2gw.loginserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.config.ConfigField;
import ru.l2gw.commons.config.ExProperties;
import ru.l2gw.commons.config.ServerConfig;
import ru.l2gw.commons.network.utils.NetList;
import ru.l2gw.commons.utils.NetUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * @author: rage
 * @date: 03.03.12 13:11
 */
public class Config extends ServerConfig
{
	protected static Log _log = LogFactory.getLog(Config.class);

	/**
	 * loginserver.properties
	 */
	@ConfigField(config = "loginserver", fieldName = "LoginserverHostname", value = "127.0.0.1")
	public static String LOGIN_HOST;
	@ConfigField(config = "loginserver", fieldName = "LoginPort", value = "9013")
	public static int GAME_SERVER_LOGIN_PORT;
	@ConfigField(config = "loginserver", fieldName = "LoginHost", value = "127.0.0.1")
	public static String GAME_SERVER_LOGIN_HOST;
	@ConfigField(config = "loginserver", fieldName = "LoginserverPort", value = "2106")
	public static int PORT_LOGIN;
	@ConfigField(config = "loginserver", fieldName = "Debug", value = "false")
	public static boolean LOGIN_DEBUG;
	@ConfigField(config = "loginserver", fieldName = "AcceptNewGameServer", value = "true")
	public static boolean ACCEPT_NEW_GAMESERVER;
	@ConfigField(config = "loginserver", value = "10")
	public static int LOGIN_TRY_BEFORE_BAN;
	@ConfigField(config = "loginserver", value = "10")
	public static int LOGIN_BAN_TIME;
	@ConfigField(config = "loginserver", value = "5")
	public static int LOGIN_CLEAR_FILED_COUNT_TIME;
	@ConfigField(config = "loginserver", fieldName = "PingServer", value = "true")
	public static boolean LOGIN_PING;
	@ConfigField(config = "loginserver", fieldName = "WaitPingTime", value = "5")
	public static int LOGIN_PING_TIME;
	@ConfigField(config = "loginserver", fieldName = "ChangePasswordAfterDays", value = "0")
	public static long PASSWORD_CHANGE_TIME;
	@ConfigField(config = "loginserver", value = "false")
	public static boolean ANTI_BRUTE_LOGIN;
	@ConfigField(config = "loginserver", fieldName = "Driver", value = "com.mysql.jdbc.Driver")
	public static String DATABASE_DRIVER;
	@ConfigField(config = "loginserver", fieldName = "URL", value = "jdbc:mysql://localhost/l2db")
	public static String DATABASE_URL;
	@ConfigField(config = "loginserver", fieldName = "Login", value = "l2db")
	public static String DATABASE_LOGIN;
	@ConfigField(config = "loginserver", fieldName = "Password", value = "")
	public static String DATABASE_PASSWORD;
	@ConfigField(config = "loginserver", fieldName = "MaximumDbConnections", value = "10")
	public static int DATABASE_MAX_CONNECTIONS;
	@ConfigField(config = "loginserver", fieldName = "MaxIdleConnectionTimeout", value = "600")
	public static int DATABASE_MAX_IDLE_TIMEOUT;
	@ConfigField(config = "loginserver", fieldName = "IdleConnectionTestPeriod", value = "60")
	public static int DATABASE_IDLE_TEST_PERIOD;
	@ConfigField(config = "loginserver", value = "true")
	public static boolean SHOW_LICENCE;
	@ConfigField(config = "loginserver", value = "true")
	public static boolean AUTO_CREATE_ACCOUNTS;
	@ConfigField(config = "loginserver", value = "[A-Za-z0-9]{3,14}")
	public static String ANAME_TEMPLATE;
	@ConfigField(config = "loginserver", value = "[A-Za-z0-9]{5,16}")
	public static String APASSWD_TEMPLATE;
	@ConfigField(config = "loginserver", fieldName = "GGCheck", value = "true")
	public static boolean LOGIN_GG_CHECK;
	@ConfigField(config = "loginserver", fieldName = "AutoRestart", value = "-1")
	public static int LRESTART_TIME;
	@ConfigField(config = "loginserver", fieldName = "AntiFloodEnable", value = "false")
	public static boolean ANTIFLOOD_ENABLE;
	@ConfigField(config = "loginserver", value = "5")
	public static int MAX_UNHANDLED_SOCKETS_PER_IP;
	@ConfigField(config = "loginserver", fieldName = "UnhandledSocketsMinTTL", value = "5000")
	public static int UNHANDLED_SOCKET_MIN_TTL;

	public static NetList INTERNAL_NETLIST = null;
	public static List<String> INTERNAL_IP = null;

	/**
	 * login_telnet.properies
	 */
	@ConfigField(config = "login_telnet", fieldName = "EnableTelnet", value = "false")
	public static boolean TELNET_ENABLED;
	@ConfigField(config = "login_telnet", fieldName = "StatusHost", value = "localhost")
	public static String TELNET_HOST;
	@ConfigField(config = "login_telnet", fieldName = "StatusPort", value = "3346")
	public static int TELNET_PORT;
	@ConfigField(config = "login_telnet", fieldName = "StatusPW", value = "password")
	public static String TELNET_PASSWORD;

    	public static void loginserverCustom(ExProperties properties)
	{
		PASSWORD_CHANGE_TIME *= 24L * 60 * 60000;
		String internalIpList = properties.getProperty("InternalIpList", "127.0.0.1,192.168.0.0-192.168.255.255,10.0.0.0-10.255.255.255,172.16.0.0-172.16.31.255");
		if(internalIpList.startsWith("NetList@"))
		{
			INTERNAL_NETLIST = new NetList();
			INTERNAL_NETLIST.LoadFromFile(internalIpList.replaceFirst("NetList@", ""));
			_log.info("Loaded " + INTERNAL_NETLIST.NetsCount() + " Internal Nets");
		}
		else
		{
			INTERNAL_IP = new ArrayList<>();
			INTERNAL_IP.addAll(Arrays.asList(internalIpList.split(",")));
		}
	}

	public static void load()
	{
		_log.info("Loading login config");
		loadConfig(Config.class, "loginserver");
		loadConfig(Config.class, "login_telnet");
	}

	public static void saveHexid(String string, String fileName)
	{
		try
		{
			Properties hexSetting = new Properties();
			File file = new File(fileName);
			file.createNewFile();
			OutputStream out = new FileOutputStream(file);
			hexSetting.setProperty("HexID", string);
			hexSetting.store(out, "the hexID to auth into login");
			out.close();
		}
		catch(Exception e)
		{
			System.out.println("Failed to save hex id to " + fileName + " File.");
			e.printStackTrace();
		}
	}

	public static byte[] generateHex(int size)
	{
		byte[] array = new byte[size];
		Random rnd = new Random();
		for(int i = 0; i < size; i++)
			array[i] = (byte) rnd.nextInt(256);

		_log.debug("Generated random String:  \"" + array + "\"");
		return array;
	}

	public static boolean isInternalIP(String ipAddress)
	{
		if(INTERNAL_NETLIST != null)
			return INTERNAL_NETLIST.isIpInNets(ipAddress);

		for(String s : INTERNAL_IP)
			if(NetUtil.checkIfIpInRange(ipAddress, s))
				return true;
		return false;
	}
}