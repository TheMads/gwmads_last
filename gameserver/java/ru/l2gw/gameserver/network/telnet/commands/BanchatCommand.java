package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.util.AutoBan;

/**
 * @author: rage
 * @date: 04.03.12 0:05
 */
public class BanchatCommand extends TelnetCommand
{
	public BanchatCommand()
	{
		super("banchat");
	}

	@Override
	public String getUsage()
	{
		return "usage: banchat <name> [minute] default is 30";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(!checkArgs(1, args))
			return null;

		int time = 30;

		if(args.length > 1)
		{
			try
			{
				time = Integer.parseInt(args[1]);
			}
			catch(Exception e)
			{
				// quite
			}
		}

		if(AutoBan.ChatBan(args[0], time, "Console", "AutoModer"))
			return "Player " + args[0] + " chat banned for " + time + " min.\n";

		return "Cannot banchat player " + args[0] + " for " + time + " min.\n";
	}
}