package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;

/**
 * @author: rage
 * @date: 03.03.12 23:11
 */
public class PerformanceCommand extends TelnetCommand
{
	public PerformanceCommand()
	{
		super("performance", "p");
	}

	@Override
	public String getUsage()
	{
		return "performance\nperformance general\nperformance npc\nperformance player";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(args.length == 0 || args[0].isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			for(String line : ThreadPoolManager.getInstance().getStats())
				sb.append(line).append("\n");

			return sb.toString();
		}

		if("general".equalsIgnoreCase(args[0]))
		{
			return ThreadPoolManager.getInstance().getGeneralPoolStats() + "\n";
		}
		if("npc".equalsIgnoreCase(args[0]))
		{
			return ThreadPoolManager.getInstance().getAIPoolStats(false) + "\n";
		}
		if("player".equals(args[0]))
		{
			return ThreadPoolManager.getInstance().getAIPoolStats(true) + "\n";
		}

		return null;
	}
}