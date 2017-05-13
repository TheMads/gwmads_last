package ru.l2gw.fakeserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.fakeserver.FakeServer;

/**
 * @author: rage
 * @date: 03.03.12 20:13
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
		return "type: status";
	}
	
	@Override
	public String handle(String[] args, String ip)
	{
		int online1, online2, store;
		int max = FakeServer.getServerManager().getMax();

		online1 = FakeServer.getServerManager().getOnline1();
		online2 = FakeServer.getServerManager().getOnline2();
		store = FakeServer.getServerManager().getStore();

		StringBuilder sb = new StringBuilder();
		
		sb.append("Fake Server Status:\n");
		sb.append(" +....... Max: ").append(max).append("\n");
		sb.append(" +.. Online 1: ").append(online1).append("\n");
		sb.append(" +.. Online 2: ").append(online2).append("\n");
		sb.append(" +..... Store: ").append(store).append("\n");
		sb.append(" +... Threads: ").append(Thread.activeCount()).append("\n");
		sb.append(" +.. RAM Used: ").append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024).append("\n");

		return sb.toString();
	}
}