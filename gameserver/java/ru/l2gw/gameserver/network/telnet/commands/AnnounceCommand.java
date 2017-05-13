package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.gameserver.Announcements;

/**
 * @author: rage
 * @date: 03.03.12 23:16
 */
public class AnnounceCommand extends TelnetCommand
{
	public AnnounceCommand()
	{
		super("announce", "a");
	}

	@Override
	public String getUsage()
	{
		return "announce <message>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(args.length < 1 || args[0].isEmpty())
			return null;

		Announcements.getInstance().announceToAll(StringUtil.joinStrings(" ", args));
		return "Send: " + StringUtil.joinStrings(" ", args);
	}
}