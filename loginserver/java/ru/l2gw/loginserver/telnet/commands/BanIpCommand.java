package ru.l2gw.loginserver.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.loginserver.IpManager;

/**
 * @author: rage
 * @date: 03.03.12 17:37
 */
public class BanIpCommand extends TelnetCommand
{
	public BanIpCommand()
	{
		super("banip");
	}

	@Override
	public String getUsage()
	{
		return "banip <ip>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(args.length < 1 || args[0].isEmpty())
			return null;

		IpManager.getInstance().BanIp(args[0], "Banned from telnet", 0, "");
		return "IP: " + args[0] + " banned.\n";
	}
}
