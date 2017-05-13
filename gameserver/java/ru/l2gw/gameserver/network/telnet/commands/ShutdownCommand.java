package ru.l2gw.gameserver.network.telnet.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.*;

/**
 * @author: rage
 * @date: 04.03.12 0:21
 */
public class ShutdownCommand extends TelnetCommand
{
	protected static final Log log = LogFactory.getLog(ShutdownCommand.class);

	public ShutdownCommand()
	{
		super("shutdown");
	}

	@Override
	public String getUsage()
	{
		return "usage: shutdown [sec] or now default is 300 sec.";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		int time = 300;

		if(checkArgs(1, args))
		{
			if("now".equals(args[0]))
			{
				try
				{
					log.warn("Shutting down via TELNET by host: " + ip);
					return "Shutting down...";
				}
				finally
				{
					System.exit(-1);
				}
			}

			try
			{
				time = Integer.parseInt(args[0]);
			}
			catch(Exception e)
			{
				// quite
			}
		}

		Shutdown.getInstance().startTelnetShutdown(ip, time, false);
		return "Server Will Shutdown In " + time + " Seconds!\nType \"abort\" To Abort Shutdown!";
	}
}