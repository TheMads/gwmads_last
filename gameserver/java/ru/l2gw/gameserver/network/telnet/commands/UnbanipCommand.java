package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.gspackets.UnbanIP;

/**
 * @author: rage
 * @date: 04.03.12 0:00
 */
public class UnbanipCommand extends TelnetCommand
{
	public UnbanipCommand()
	{
		super("unbanip");
	}

	@Override
	public String getUsage()
	{
		return "usage: unbanip <ip>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(!checkArgs(1, args))
			return null;

		LSConnection.getInstance().sendPacket(new UnbanIP(args[0]));
		return "IP: " + args[0] + " unbanned\n";
	}
}
