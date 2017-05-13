package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.GameServer;

/**
 * @author: rage
 * @date: 04.03.12 0:36
 */
public class VersionCommand extends TelnetCommand
{
	public VersionCommand()
	{
		super("version", "ver");
	}

	@Override
	public String getUsage()
	{
		return "usage: version";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		return "L2GW server. Running " + GameServer.getVersion().getRevisionNumber() + "/" + GameServer.getVersion().getBuildDate() + "\n";
	}
}
