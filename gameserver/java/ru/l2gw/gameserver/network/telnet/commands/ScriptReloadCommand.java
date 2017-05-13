package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.extensions.scripts.Scripts;

/**
 * @author: rage
 * @date: 17.01.13 11:42
 */
public class ScriptReloadCommand extends TelnetCommand
{
	public ScriptReloadCommand()
	{
		super("sreload");
	}

	@Override
	public String getUsage()
	{
		return "usage: sreload [script]\n";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(args.length < 1)
		{
			if(Scripts.getInstance().reload())
				return "Scripts reloaded with errors. Loaded " + Scripts.getInstance().getClasses().size() + " classes.";

			return "Scripts successfully reloaded. Loaded " + Scripts.getInstance().getClasses().size() + " classes.";
		}
		else if(checkArgs(1, args))
		{
			if(Scripts.getInstance().reloadClass(args[0]))
				return "Scripts reloaded with errors. Loaded " + Scripts.getInstance().getClasses().size() + " classes.";
			else
				return "Scripts successfully reloaded. Loaded " + Scripts.getInstance().getClasses().size() + " classes.";
		}

		return getUsage();
	}
}