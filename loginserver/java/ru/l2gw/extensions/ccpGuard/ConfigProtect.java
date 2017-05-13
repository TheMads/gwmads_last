package ru.l2gw.extensions.ccpGuard;

import ru.l2gw.commons.config.ConfigField;
import ru.l2gw.commons.config.ExProperties;
import ru.l2gw.commons.config.ServerConfig;
import ru.l2gw.commons.network.utils.NetList;

import java.io.File;

/**
 * @author: rage
 * @date: 03.03.12 16:16
 */
public class ConfigProtect extends ServerConfig
{
	/** Argot's Protection config */
	public static final String PROTECT_FILE = "config/protection.properties";

	@ConfigField(config = "protection", fieldName = "EnableProtect", value = "false")
	public static boolean PROTECT_ENABLE;
	@ConfigField(config = "protection", value = "false")
	public static boolean PROTECT_DEBUG;
	@ConfigField(config = "protection", value = "false")
	public static boolean PROTECT_LOGIN_ANTIBRUTE;
	@ConfigField(config = "protection", value = "0x0000c621")
	public static int PROTECT_LOGIN_PROTOCOL_VERSION = 0x0000c621;

	public static NetList PROTECT_UNPROTECTED_IPS;

	public static void protectionCustom(ExProperties properties)
	{
		String ips = properties.getProperty("UpProtectedIPs", "");
		if(!ips.equals(""))
			PROTECT_UNPROTECTED_IPS.LoadFromString(ips, ",");
	}

	public static void load()
	{
		File fp = new File(PROTECT_FILE);
		PROTECT_ENABLE = fp.exists();
		if(PROTECT_ENABLE)
		{
			loadConfig(ConfigProtect.class, "protection");
		}
	}
}