package ru.l2gw.gameserver.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.extensions.scripts.Functions;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminCommandHandler
{
	private static Log log = LogFactory.getLog("admincmd");
	private static final Pattern COMMAND_ARGS_PATTERN = Pattern.compile("\"([^\"]*)\"|([^\\s]+)");
	private static AdminCommandHandler instance;

	private static final Map<String, IAdminCommandHandler> commandHandlers = new HashMap<>();
	private static final Map<String, String> commandUsage = new HashMap<>();

	public static AdminCommandHandler getInstance()
	{
		if(instance == null)
			instance = new AdminCommandHandler();
		return instance;
	}

	public void registerAdminCommandHandler(IAdminCommandHandler handler)
	{
		try
		{
			if(handler.getAdminCommandList().length < 1)
			{
				log.warn("Incorrect help imlemented in class: " + handler.getClass().getName() + " no command list");
				return;
			}
		}
		catch(Exception e)
		{
			log.warn("Incorrect help imlemented in class: " + handler.getClass().getName() + " null command list");
			return;
		}

		for(AdminCommandDescription command : handler.getAdminCommandList())
		{
			log.debug("Adding handler for command: " + command.command);
			commandHandlers.put(command.command.toLowerCase(), handler);
			commandUsage.put(command.command, command.usage);
		}
	}

	public void unregisterAdminCommandHandler(IAdminCommandHandler handler)
	{
		for(AdminCommandDescription command : handler.getAdminCommandList())
		{
			log.debug("Remove handler for command: " + command.command);
			commandHandlers.remove(command.command);
			commandUsage.remove(command.command);
		}
	}

	public String getCommandUsage(String command)
	{
		if(commandUsage.containsKey(command))
			return commandUsage.get(command);
		return "Command not found";
	}

	/**
	 * @return размер комманд
	 */
	public int size()
	{
		return commandHandlers.size();
	}

	public void useAdminCommandHandler(L2Player player, String adminCommand)
	{
		Matcher m = COMMAND_ARGS_PATTERN.matcher(adminCommand);

		m.find();
		String command = m.group();

		List<String> args = new ArrayList<>();
		String arg;
		while(m.find())
		{
			arg = m.group(1);
			if(arg == null)
				arg = m.group(0);
			args.add(arg);
		}

		if(!command.startsWith("admin_"))
			command = "admin_" + command;

		if(!AdminTemplateManager.checkBoolean("useCommands", player))
		{
			Functions.sendSysMessage(player, new CustomMessage("ru.l2gw.gameserver.clientpackets.SendBypassBuildCmd.NoCommandOrAccess", player).addString(command.replace("admin_", "//")).toString());
			return;
		}

		if(Config.DEBUG)
			log.debug("getting handler for command: " + command + " -> " + (commandHandlers.get(command) != null));

		IAdminCommandHandler ach = commandHandlers.get(command);

		if(ach != null)
		{
			if(!AdminTemplateManager.checkCommandAllow(command, player))
			{
				log.info(player.toFullString() + " cmd{" + adminCommand.replace("admin_", "") + "}" + (player.getTarget() == null ? "" : " target: " + player.getTarget()) + " success: deny command.");
				Functions.sendSysMessage(player, new CustomMessage("ru.l2gw.gameserver.clientpackets.SendBypassBuildCmd.NoCommandOrAccess", player).addString(command.replace("admin_", "")).toString());
				return;
			}

			boolean command_success = false;
			try
			{
				command_success = ach.useAdminCommand(command.toLowerCase(), args.toArray(new String[args.size()]), adminCommand, player);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			log.info(player.toFullString() + " cmd{" + adminCommand.replace("admin_", "") + "}" + (player.getTarget() == null ? "" : " target: " + player.getTarget()) + " success: " + command_success);
		}
		else
		{
			Functions.sendSysMessage(player, "Command: " + command.replace("admin_", "//") + " not found.");
		}
	}
}