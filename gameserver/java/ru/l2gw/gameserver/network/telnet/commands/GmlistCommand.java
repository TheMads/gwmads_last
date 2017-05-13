package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.tables.GmListTable;

/**
 * @author: rage
 * @date: 03.03.12 23:30
 */
public class GmlistCommand extends TelnetCommand
{
	public GmlistCommand()
	{
		super("gmlist");
	}

	@Override
	public String getUsage()
	{
		return "usage: gmlist";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("There are currently ").append(GmListTable.getAllGMs().size()).append(" GM(s) online...\n");

		for(L2Player player : GmListTable.getAllGMs())
		{
			sb.append(player.getName()).append("\n");
		}

		return sb.toString();
	}
}