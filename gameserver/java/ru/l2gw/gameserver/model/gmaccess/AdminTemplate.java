package ru.l2gw.gameserver.model.gmaccess;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.templates.StatsSet;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: rage
 * @date: 09.03.12 12:47
 */
public class AdminTemplate
{
	private final String name;
	private GArray<String> allowCommandList;
	private GArray<String> denyCommandList;
	private GArray<AdminCommandLimit> limits;
	private Map<String, GArray<AdminCommandLimit>> propertyLimits;
	private final StatsSet properties = new StatsSet();

	public AdminTemplate(String name)
	{
		this.name = name;
	}

	public void addLimit(AdminCommandLimit limit)
	{
		if(limits == null)
			limits = new GArray<>();

		limits.add(limit);
	}
	
	public void addPropertyLimit(String property, AdminCommandLimit limit)
	{
		if(propertyLimits == null)
			propertyLimits = new HashMap<>();
		
		GArray<AdminCommandLimit> limits = propertyLimits.get(property);
		if(limits == null)
		{
			limits = new GArray<>();
			propertyLimits.put(property, limits);
		}

		limits.add(limit);
	}

	public void setAllowCommands(GArray<String> allow)
	{
		allowCommandList = allow;
	}

	public void setDenyCommands(GArray<String> deny)
	{
		denyCommandList = deny;
	}

	public void setProperty(String name, String value)
	{
		properties.set(name, value);
	}

	public StatsSet getProperties()
	{
		return properties;
	}

	public boolean checkCommand(String command)
	{
		if(denyCommandList == null)
			return allowCommandList == null || allowCommandList.contains(command.replace("admin_", ""));

		return !denyCommandList.contains(command.replace("admin_", "")) && (allowCommandList == null || allowCommandList.contains(command.replace("admin_", "")));
	}

	public boolean checkLimits(String command, L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		if(limits == null)
			return true;

		for(AdminCommandLimit limit : limits)
		{
			if(limit.commandMatch(command) && !limit.checkLimit(player, target, arg1, arg2, arg3))
				return false;
		}

		return true;
	}
	
	public boolean checkBoolean(String property, L2Player player)
	{
		if(propertyLimits != null && propertyLimits.containsKey(property))
		{
			for(AdminCommandLimit limit : propertyLimits.get(property))
			{
				if(!limit.checkLimit(player, player, null, null, null))
					return false;
			}
		}

		return properties.getBool(property, false);
	}
	
	@Override
	public String toString()
	{
		return "AdminTemplate[" + name + "]";
	}
}
