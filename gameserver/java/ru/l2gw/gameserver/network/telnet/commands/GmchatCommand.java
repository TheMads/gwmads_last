package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.gameserver.serverpackets.Say2;
import ru.l2gw.gameserver.tables.GmListTable;

/**
 * @author: rage
 * @date: 03.03.12 23:27
 */
public class GmchatCommand extends TelnetCommand
{
	public GmchatCommand()
	{
		super("gmchat");
	}

	@Override
	public String getUsage()
	{
		return "";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(args.length < 1 || args[0].isEmpty())
			return null;

		GmListTable.broadcastToGMs(new Say2(0, 9, "Telnet GM Broadcast(" + ip + ")", StringUtil.joinStrings(" ", args)));
		return "Your Message Has Been Sent To " + GmListTable.getAllGMs().size() + " GM(s).";
	}
}
