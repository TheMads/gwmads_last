package ru.l2gw.loginserver.telnet.commands;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.commons.network.utils.BannedIp;
import ru.l2gw.loginserver.IpManager;

/**
 * @author: rage
 * @date: 03.03.12 17:25
 */
public class BanIpListCommand extends TelnetCommand
{
	public BanIpListCommand()
	{
		super("baniplist");
	}

	@Override
	public String getUsage()
	{
		return "type: baniplist";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		GArray<BannedIp> banList = IpManager.getInstance().getBanList();
		if(banList.isEmpty())
			return "Ban IP list ie empty\n";

		StringBuilder sb = new StringBuilder();
		sb.append("Ban IP List:\n");
		for(BannedIp temp : banList)
			sb.append("IP: ").append(temp.ip).append(" banned by ").append(temp.admin).append("\n");

		return sb.toString();
	}
}
