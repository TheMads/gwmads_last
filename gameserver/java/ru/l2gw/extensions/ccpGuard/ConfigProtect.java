package ru.l2gw.extensions.ccpGuard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.utils.NetList;
import ru.l2gw.extensions.ccpGuard.crypt.ProtectionCrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class ConfigProtect
{
	protected static Log _log = LogFactory.getLog(ConfigProtect.class.getName());

	/** Argot's Protection config */
	public static boolean PROTECT_ENABLE;
	public static boolean PROTECT_DEBUG;
	public static NetList PROTECT_UNPROTECTED_IPS;
	public static boolean PROTECT_GS_STORE_HWID;
	public static boolean PROTECT_GS_LOG_HWID;
	public static boolean PROTECT_ENABLE_HWID_BANS;
	public static int PROTECT_CONST_VALUE;
	public static int PROTECT_WINDOWS_COUNT;
	public static int PROTECT_PROTOCOL_VERSION;
	public static boolean PROTECT_LOGIN_ANTIBRUTE;
	public static int PROTECT_LOGIN_PROTOCOL_VERSION = 0x0000c621;
	public static boolean PROTECT_KICK_WITH_EMPTY_HWID;
	public static boolean PROTECT_KICK_WITH_LASTERROR_HWID;
	public static boolean PROTECT_ENABLE_HWID_LOCK;
	public static boolean PROTECT_ENABLE_GG_SYSTEM;
	public static long PROTECT_GG_SEND_INTERVAL;
	public static long PROTECT_GG_RECV_INTERVAL;
	public static long PROTECT_TASK_GG_INVERVAL;
	public static int PROTECT_TOTAL_PENALTY;
	public static String PROTECT_HTML_SHOW;
	public static int PROTECT_PENALTY_IG;
	public static int PROTECT_PENALTY_ACP;
	public static int PROTECT_PENALTY_CONSOLE_CMD;
	public static int PROTECT_PENALTY_L2PHX;
	public static int PROTECT_PENALTY_L2CONTROL;
	public static int PROTECT_PENALTY_BOT;
	public static int PROTECT_PUNISHMENT_ILLEGAL_SOFT;
	public static boolean PROTECT_HWID_PROBLEM_RESOLVED;
	public static boolean PROTECT_SHOW_PING;
	public static int PROTECT_ONLINE_PACKET_TIME;
	public static int PROTECT_TITLE_X;
	public static int PROTECT_TITLE_Y;
	public static int PROTECT_TITLE_COLOR;
	public static int PROTECT_ONLINE_COLOR;
	public static int PROTECT_PING_COLOR;
	public static String PROTECT_SERVER_TITLE;
	public static final String PROTECT_FILE = "./config/protection.properties";

	public static void load()
	{
		File fp = new File(PROTECT_FILE);
		PROTECT_ENABLE = fp.exists();
		if(PROTECT_ENABLE)
			try
			{
				Properties protectSettings = new Properties();
				InputStream is = new FileInputStream(fp);
				protectSettings.load(is);
				is.close();
				PROTECT_UNPROTECTED_IPS = new NetList();

				String _ips = getProperty(protectSettings, "UpProtectedIPs", "");
				if(!_ips.equals(""))
					PROTECT_UNPROTECTED_IPS.LoadFromString(_ips, ",");

				//возможность отключить защиту при наличии файла PROTECT_FILE
				PROTECT_ENABLE = getBooleanProperty(protectSettings, "EnableProtect", true);
				//не выносить в конфиг, только при необходимости
				PROTECT_CONST_VALUE = getIntProperty(protectSettings, "ServerConst", 0);
				//ограничение окон для одного компьютера
				PROTECT_WINDOWS_COUNT = getIntProperty(protectSettings, "AllowedWindowsCount", 99);
				PROTECT_PROTOCOL_VERSION = getIntProperty(protectSettings, "ProtectProtocolVersion", 777);
				PROTECT_LOGIN_ANTIBRUTE = getBooleanProperty(protectSettings, "ProtectLoginAntibrute", false);
				PROTECT_LOGIN_PROTOCOL_VERSION = getIntProperty(protectSettings, "ProtectLoginProtocolVersion", 0x0000c621);
				PROTECT_KICK_WITH_EMPTY_HWID = getBooleanProperty(protectSettings, "KickWithEmptyHWID", true);
				PROTECT_KICK_WITH_LASTERROR_HWID = getBooleanProperty(protectSettings, "KickWithLastErrorHWID", false);
				PROTECT_ENABLE_HWID_LOCK = getBooleanProperty(protectSettings, "EnableHWIDLock", false);
				PROTECT_ENABLE_GG_SYSTEM = getBooleanProperty(protectSettings, "EnableGGSystem", true);
				PROTECT_DEBUG = getBooleanProperty(protectSettings, "ProtectDebug", false);
				//не выносить в конфиг. сообщать о ней, только в случае проблем
				PROTECT_HWID_PROBLEM_RESOLVED = getBooleanProperty(protectSettings, "HwidProblemResolved", false);
				//ToDo
				PROTECT_GG_SEND_INTERVAL = getLongProperty(protectSettings, "GGSendInterval", 60000);
				PROTECT_GG_RECV_INTERVAL = getLongProperty(protectSettings, "GGRecvInterval", 8000);
				PROTECT_TASK_GG_INVERVAL = getLongProperty(protectSettings, "GGTaskInterval", 5000);
				PROTECT_TOTAL_PENALTY = getIntProperty(protectSettings, "TotalPenaltyPoint", 10);
				PROTECT_HTML_SHOW = getProperty(protectSettings, "ShowHtml", "none");
				PROTECT_PUNISHMENT_ILLEGAL_SOFT = getIntProperty(protectSettings, "PunishmentIllegalSoft", 1);
				PROTECT_PENALTY_IG = getIntProperty(protectSettings, "PenaltyIG", 10);
				PROTECT_PENALTY_L2PHX = getIntProperty(protectSettings, "PenaltyL2phx", 10);
				PROTECT_PENALTY_L2CONTROL = getIntProperty(protectSettings, "PenaltyL2Control", 10);
				PROTECT_PENALTY_BOT = getIntProperty(protectSettings, "PenaltyBot", 10);
				PROTECT_PENALTY_CONSOLE_CMD = getIntProperty(protectSettings, "PenaltyConsoleCMD", 1);
				PROTECT_ONLINE_PACKET_TIME = getIntProperty(protectSettings, "OnlinePacketTime", 0);
				PROTECT_GS_STORE_HWID = getBooleanProperty(protectSettings, "StoreHWID", true);
				PROTECT_GS_LOG_HWID = getBooleanProperty(protectSettings, "LogHWID", true);
				PROTECT_SHOW_PING = getBooleanProperty(protectSettings, "ShowPing", false);
				PROTECT_TITLE_X = getIntProperty(protectSettings, "TitleX", 240);
				PROTECT_TITLE_Y = getIntProperty(protectSettings, "TitleY", 8);
				PROTECT_SERVER_TITLE = getProperty(protectSettings, "ServerTitle", "");
				PROTECT_TITLE_COLOR = Integer.decode("0x" + getProperty(protectSettings, "ServerTitleColor", "00FF00")) | 0xFF000000;
				PROTECT_ONLINE_COLOR = Integer.decode("0x" + getProperty(protectSettings, "OnlineColor", "00FF00")) | 0xFF000000;
				PROTECT_PING_COLOR = Integer.decode("0x" + getProperty(protectSettings, "PingColor", "00FF00")) | 0xFF000000;
				ProtectionCrypt.loadProtectData();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}

	private static String getProperty(Properties prop, String name)
	{
		return prop.getProperty(name.trim(), null);
	}

	private static String getProperty(Properties prop, String name, String _default)
	{
		String s = getProperty(prop, name);
		return s == null ? _default : s;
	}

	private static int getIntProperty(Properties prop, String name, int _default)
	{
		String s = getProperty(prop, name);
		return s == null ? _default : Integer.parseInt(s.trim());
	}

	private static int getIntHexProperty(Properties prop, String name, int _default)
	{
		String s = getProperty(prop, name);
		if(s == null)
			return _default;
		s = s.trim();
		if(!s.startsWith("0x"))
			s = "0x" + s;
		return Integer.decode(s);
	}

	private static long getLongProperty(Properties prop, String name, long _default)
	{
		String s = getProperty(prop, name);
		return s == null ? _default : Long.parseLong(s.trim());
	}

	private static byte getByteProperty(Properties prop, String name, byte _default)
	{
		String s = getProperty(prop, name);
		return s == null ? _default : Byte.parseByte(s.trim());
	}

	private static byte getByteProperty(Properties prop, String name, int _default)
	{
		return getByteProperty(prop, name, (byte) _default);
	}

	private static boolean getBooleanProperty(Properties prop, String name, boolean _default)
	{
		String s = getProperty(prop, name);
		return s == null ? _default : Boolean.parseBoolean(s.trim());
	}

	private static float getFloatProperty(Properties prop, String name, float _default)
	{
		String s = getProperty(prop, name);
		return s == null ? _default : Float.parseFloat(s.trim());
	}

	private static float getFloatProperty(Properties prop, String name, double _default)
	{
		return getFloatProperty(prop, name, (float) _default);
	}

	private static double getDoubleProperty(Properties prop, String name, double _default)
	{
		String s = getProperty(prop, name);
		return s == null ? _default : Double.parseDouble(s.trim());
	}

	// it has no instancies
	private ConfigProtect()
	{}

}