package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.Shutdown;

/**
 * @author: rage
 * @date: 04.03.12 0:30
 */
public class RestartCommand extends TelnetCommand
{
	public RestartCommand()
	{
		super("restart");
	}

	@Override
	public String getUsage()
	{
		return "usage: restart <sec> default is 300 seconds.";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		int time = 300;
		if(checkArgs(1, args))
		{
			try
			{
				time = Integer.parseInt(args[0]);
			}
			catch(Exception e)
			{
				return "Invalid value: " + args[0] + "\n";
			}
		}

		Shutdown.getInstance().startTelnetShutdown(ip, time, true);
		return "Server Will Restart In " + time + " Seconds!\nType \"abort\" To Abort Restart!";
	}
}