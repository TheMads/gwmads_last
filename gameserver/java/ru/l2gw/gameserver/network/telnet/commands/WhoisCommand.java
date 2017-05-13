package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author: rage
 * @date: 03.03.12 23:34
 */
public class WhoisCommand extends TelnetCommand
{
	public WhoisCommand()
	{
		super("whois");
	}

	@Override
	public String getUsage()
	{
		return "usage: whois <name>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(args.length < 1 || args[0].isEmpty())
			return null;

		L2Player player = L2ObjectsStorage.getPlayer(args[0]);
		if(player != null)
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Name:").append(player.getName()).append("\n");
			sb.append("Account:").append(player.getAccountName()).append("\n");
			sb.append("IP:").append(player.getNetConnection().getIpAddr()).append("\n");
			sb.append("Level:").append(player.getLevel()).append("\n");
			if(player.getNetConnection() != null)
				sb.append("HWID:").append(player.getNetConnection()._prot_info.getHWID()).append("\n");
			return sb.toString();
		}

		return "No such player online!";
	}
}