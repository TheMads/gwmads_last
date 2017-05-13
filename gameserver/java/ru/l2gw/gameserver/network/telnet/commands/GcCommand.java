package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;

/**
 * @author: rage
 * @date: 04.03.12 0:34
 */
public class GcCommand extends TelnetCommand
{
	public GcCommand()
	{
		super("gc");
	}

	@Override
	public String getUsage()
	{
		return "usage: gc";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		try
		{
			System.gc();
			Thread.sleep(1000L);
			System.gc();
			Thread.sleep(1000L);
			System.gc();
		}
		catch(Exception e)
		{
			// quite
		}

		return "OK! - garbage collector called.\n";
	}
}