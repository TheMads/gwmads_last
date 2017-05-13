package ru.l2gw.gameserver.handler;

/**
 * @author: rage
 * @date: 15.03.12 14:16
 */
public class AdminCommandDescription
{
	public final String command;
	public final String usage;
	
	public AdminCommandDescription(String command, String usage)
	{
		this.command = command;
		this.usage = usage == null ? "usage: //" + command.replace("admin_", "") : usage;
	}
}