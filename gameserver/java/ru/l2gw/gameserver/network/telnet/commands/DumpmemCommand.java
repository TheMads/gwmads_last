package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.debug.HeapDumper;
import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.Config;

/**
 * @author: rage
 * @date: 04.03.12 0:48
 */
public class DumpmemCommand extends TelnetCommand
{
	public DumpmemCommand()
	{
		super("dumpmem");
	}

	@Override
	public String getUsage()
	{
		return "usage: dumpmem [dir] [live] false/true";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		String dir = checkArgs(1, args) ? args[0] : Config.DEBUG_DUMPMEMDIR;
		boolean live = checkArgs(2, args) && Boolean.getBoolean(args[1]);
		return "Dump memory to: " + HeapDumper.dumpHeap(dir, live) + "\n";
	}
}