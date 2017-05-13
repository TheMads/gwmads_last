package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.gspackets.BanIP;

/**
 * @author: rage
 * @date: 03.03.12 23:40
 */
public class BanipCommand extends TelnetCommand
{
	public BanipCommand()
	{
		super("banip");
	}

	@Override
	public String getUsage()
	{
		return "usage: banip <ip>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(!checkArgs(1, args))
			return null;

		LSConnection.getInstance().sendPacket(new BanIP(args[0], "Telnet: " + ip));
		return "IP: " + args[0] + " banned.";
	}
}
