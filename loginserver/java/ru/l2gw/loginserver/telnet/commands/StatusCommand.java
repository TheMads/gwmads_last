package ru.l2gw.loginserver.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.loginserver.GameServerTable;

/**
 * @author: rage
 * @date: 03.03.12 17:11
 */
public class StatusCommand extends TelnetCommand
{
	public StatusCommand()
	{
		super("status", "s");
	}

	@Override
	public String getUsage()
	{
		return "just type: status";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		StringBuilder sb = new StringBuilder();
		for(String str : GameServerTable.getInstance().status())
			sb.append(str).append("\n");

		return sb.toString();
	}
}
