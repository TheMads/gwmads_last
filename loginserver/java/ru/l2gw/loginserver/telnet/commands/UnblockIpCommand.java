package ru.l2gw.loginserver.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.loginserver.L2LoginServer;

/**
 * @author: rage
 * @date: 03.03.12 17:16
 */
public class UnblockIpCommand extends TelnetCommand
{
	public UnblockIpCommand()
	{
		super("unblockip", "ubip");
	}

	@Override
	public String getUsage()
	{
		return "unblockip <ip>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(args.length < 1 || args[0].isEmpty())
			return null;

		if(L2LoginServer.getInstance().unblockIp(args[0]))
		{
			log.warn("IP removed via LS TELNET");
			return "The IP " + args[0] + " has been removed from the hack protection list!\n";
		}

		return "IP not found in hack protection list...\n";
	}
}
