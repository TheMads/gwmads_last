package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.util.Location;

/**
 * @author: rage
 * @date: 04.03.12 0:41
 */
public class SpawnItemCommand extends TelnetCommand
{
	public SpawnItemCommand()
	{
		super("spawnitem");
	}

	@Override
	public String getUsage()
	{
		return "usage: spawnitem <name> <itemId> <amount>";
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
				for(int i = 0; i < amount; i++)
				{
					Location loc = GeoEngine.findPointToStay(player.getX(), player.getY(), player.getZ(), 50, 100, player.getReflection());
					L2ItemInstance item = ItemTable.getInstance().createItem("SpawnFromTelnet", itemId, 1, player);
					item.setReflection(player.getReflection());
					if(item.isStackable())
					{
						item.setCount(amount);
						item.dropMe(null, loc);
						break;
					}
					else
						item.dropMe(null, loc);
				}
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
