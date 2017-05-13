package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author: rage
 * @date: 04.03.12 0:37
 */
public class GiveCommand extends TelnetCommand
{
	public GiveCommand()
	{
		super("give");
	}

	@Override
	public String getUsage()
	{
		return "usage: give <name> <itemId> <amount>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(!checkArgs(3, args))
			return null;

		try
		{
			L2Player player = L2ObjectsStorage.getPlayer(args[0]);
			int itemId = Integer.parseInt(args[1]);
			int amount = Integer.parseInt(args[2]);

			if(player != null)
			{
				player.addItem("GiveFromTelnet", itemId, amount, null, true);
				return "ok\n";
			}

			return "player: " + args[0] + " not found.\n";
		}
		catch(Exception e)
		{
			return "Syntax error.\n";
		}
	}
}
