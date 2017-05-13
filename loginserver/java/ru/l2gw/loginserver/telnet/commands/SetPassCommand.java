package ru.l2gw.loginserver.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.loginserver.L2LoginServer;

/**
 * @author: rage
 * @date: 03.03.12 17:42
 */
public class SetPassCommand extends TelnetCommand
{
	public SetPassCommand()
	{
		super("setpass");
	}

	@Override
	public String getUsage()
	{
		return "setpass <account> <password>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(args.length < 2 || args[0].isEmpty() || args[1].isEmpty())
			return null;

		if(L2LoginServer.getInstance().setPassword(args[0], args[1]))
			return "Password for account: " + args[0] + " successfully changed.\n";

		return "Error while set new password.\n";
	}
}
