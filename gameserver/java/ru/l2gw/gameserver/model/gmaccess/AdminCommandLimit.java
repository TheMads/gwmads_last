package ru.l2gw.gameserver.model.gmaccess;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author: rage
 * @date: 09.03.12 12:47
 */
public class AdminCommandLimit
{
	private final String commandMatch;
	private GArray<IAdminLimit> limits;
	private final GArray<String> commands;
	
	public AdminCommandLimit(String command, GArray<String> commands)
	{
		commandMatch = command;
		this.commands = commands;
	}

	public void addCommandLimits(GArray<IAdminLimit> limits)
	{
		if(!limits.isEmpty())
			this.limits = limits;
	}

	public boolean commandMatch(String command)
	{
		if(commands != null)
			return commands.contains(command.replace("admin_", ""));

		return command.replace("admin_", "").startsWith(commandMatch);
	}

	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		if(limits == null)
			return true;

		for(IAdminLimit limit : limits)
			if(!limit.checkLimit(player, target, arg1, arg2, arg3))
				return false;

		return true;
	}
}
