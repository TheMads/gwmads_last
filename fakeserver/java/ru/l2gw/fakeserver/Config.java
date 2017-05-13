package ru.l2gw.fakeserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.config.ConfigField;
import ru.l2gw.commons.config.ExProperties;
import ru.l2gw.commons.config.ServerConfig;
import ru.l2gw.fakeserver.manager.ServerInfo;

/**
 * @author: rage
 * @date: 18.04.13 13:19
 */
public class Config extends ServerConfig
{
	protected static Log _log = LogFactory.getLog(Config.class);

	@ConfigField(config = "server", value = "4")
	public static int GENERAL_PACKET_THREAD_CORE_SIZE;
	@ConfigField(config = "server", value = "127.0.0.1")
	public static String FAKESERVER_HOSTNAME;
	@ConfigField(config = "server", fieldName = "FakeserverPort", value = "7777")
	public static int PORT_GAME;
	@ConfigField(config = "server", value = "")
	public static String[] SERVER_LIST;
	@ConfigField(config = "server", value = "300000")
	public static long REQUEST_DELAY;
	@ConfigField(config = "server", value = "true")
	public static boolean TELNET_ENABLED;
	@ConfigField(config = "server", value = "3360")
	public static int TELNET_PORT;
	@ConfigField(config = "server", value = "127.0.0.1")
	public static String TELNET_HOST;
	@ConfigField(config = "server", value = "")
	public static String TELNET_PASSWORD;

	public static GArray<ServerInfo> SERVERS;

	public static void load()
	{
		_log.info("Loading config.");
		loadConfig(Config.class, "server");
	}

	public static void serverCustom(ExProperties properties)
	{
		SERVERS = new GArray<>(SERVER_LIST.length);
		for(String info : SERVER_LIST)
		{
			if(info.contains(":"))
				SERVERS.add(new ServerInfo(info.split(":")[0], Integer.parseInt(info.split(":")[1])));
			else
				SERVERS.add(new ServerInfo(info, 7777));
		}
	}
}
