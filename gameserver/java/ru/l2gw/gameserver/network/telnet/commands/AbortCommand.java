package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.*;

/**
 * @author: rage
 * @date: 04.03.12 0:32
 */
public class AbortCommand extends TelnetCommand
{
	public AbortCommand()
	{
		super("abort");
	}

	@Override
	public String getUsage()
	{
		return "usage: abort";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		Shutdown.getInstance().telnetAbort(ip);
		return "OK! - Shutdown/Restart Aborted.\n";
	}
}