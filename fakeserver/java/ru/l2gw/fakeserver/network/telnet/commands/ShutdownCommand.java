package ru.l2gw.fakeserver.network.telnet.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.telnet.TelnetCommand;

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
}