package ru.l2gw.loginserver.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.loginserver.L2LoginServer;

/**
 * @author: rage
 * @date: 03.03.12 17:34
 */
public class ShutdownCommand extends TelnetCommand
{
	public ShutdownCommand()
	{
		super("shutdown");
	}

	@Override
	public String getUsage()
	{
		return "type: shutdown";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		L2LoginServer.getInstance().shutdown(false);
		return "Shutdown login server...";
	}
}
