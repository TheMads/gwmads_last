package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;

/**
 * @author: rage
 * @date: 04.03.12 0:51
 */
public class PurgeCommand extends TelnetCommand
{
	public PurgeCommand()
	{
		super("purge");
	}

	@Override
	public String getUsage()
	{
		return "usage: purge";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		ThreadPoolManager.getInstance().purge();
		return "Purge threads ok.\n";
	}
}
