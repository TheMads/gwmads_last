package ru.l2gw.loginserver.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.loginserver.L2LoginServer;

/**
 * @author: rage
 * @date: 03.03.12 17:36
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
		return "type: restart";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		L2LoginServer.getInstance().shutdown(true);
		return "Restarting login server...";
	}
}
