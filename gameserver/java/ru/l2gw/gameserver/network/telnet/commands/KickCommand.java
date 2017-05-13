package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author: rage
 * @date: 04.03.12 0:02
 */
public class KickCommand extends TelnetCommand
{
	public KickCommand()
	{
		super("kick");
	}

	@Override
	public String getUsage()
	{
		return "usage: kick <name> or <objectId>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(!checkArgs(1, args))
			return null;

		L2Player player = L2ObjectsStorage.getPlayer(args[0]);
		if(player == null)
		{
			try
			{
				player = L2ObjectsStorage.getPlayer(Integer.parseInt(args[0]));
			}
			catch(Exception e)
			{
				// quite
			}
		}

		if(player != null)
		{
			if(player.isInOfflineMode())
				player.setOfflineMode(false);
			player.logout(false, false, true);
			return "Player: " + args[0] + " kicked\n";
		}

		return "Player: " + args[0] + " not online\n";
	}
}
