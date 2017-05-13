package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.tables.NpcTable;

/**
 * @author: rage
 * @date: 04.03.12 0:44
 */
public class ShowCommand extends TelnetCommand
{
	public ShowCommand()
	{
		super("show");
	}

	@Override
	public String getUsage()
	{
		return "show drop <npcId>\nshow config <name>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(!checkArgs(2, args))
			return null;

		try
		{
			if(args[0].equals("drop"))
			{
				int npcId = Integer.parseInt(args[1]);
				GArray<String> drop = NpcTable.generateDroplistString(npcId);
				StringBuilder sb = new StringBuilder();
				for(String s : drop)
					sb.append(s).append("\n");

				return sb.toString();
			}
			else if(args[0].equals("config"))
			{
				return Config.showConfig(Config.class, args[1]);
			}
		}
		catch(Exception e)
		{
			// Quite
		}
		return "Syntax error.\n";
	}
}