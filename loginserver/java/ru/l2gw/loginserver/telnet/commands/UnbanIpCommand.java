package ru.l2gw.loginserver.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.loginserver.IpManager;

/**
 * @author: rage
 * @date: 03.03.12 17:41
 */
public class UnbanIpCommand extends TelnetCommand
{
	public UnbanIpCommand()
	{
		super("unbanip");
	}

	@Override
	public String getUsage()
	{
		return "unbanip <ip>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(args.length < 1 || args[0].isEmpty())
			return null;

		IpManager.getInstance().UnbanIp(args[0]);
		return "IP: " + args[0] + " unbanned.\n";
	}
}
