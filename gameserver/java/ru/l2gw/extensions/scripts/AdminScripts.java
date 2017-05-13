package ru.l2gw.extensions.scripts;

import ru.l2gw.gameserver.handler.AdminCommandDescription;
import ru.l2gw.gameserver.handler.IAdminCommandHandler;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.model.quest.QuestState;

public class AdminScripts implements IAdminCommandHandler
{
	private static AdminCommandDescription[] _adminCommands =
			{ 
					new AdminCommandDescription("admin_scripts_reload", null),
					new AdminCommandDescription("admin_sreload", null), 
					new AdminCommandDescription("admin_sqreload", null) 
			};

	@Override
	public boolean useAdminCommand(String command, String[] args, String fullCommand, L2Player player)
	{
		if(!AdminTemplateManager.checkCommand(command, player, null, null, null, null))
		{
			Functions.sendSysMessage(player, "Access deined.");
			return false;
		}

		if(command.equals("admin_scripts_reload") || command.equals("admin_sreload"))
		{
			if(args.length < 1)
				return false;
			String param = args[0];
			if(param.equalsIgnoreCase("all"))
			{
				player.sendMessage("Scripts reload starting...");
				if(Scripts.getInstance().reload())
					player.sendMessage("Scripts reloaded with errors. Loaded " + Scripts.getInstance().getClasses().size() + " classes.");
				else
					player.sendMessage("Scripts successfully reloaded. Loaded " + Scripts.getInstance().getClasses().size() + " classes.");
			}
			else if(Scripts.getInstance().reloadClass(param))
				player.sendMessage("Scripts reloaded with errors. Loaded " + Scripts.getInstance().getClasses().size() + " classes.");
			else
				player.sendMessage("Scripts successfully reloaded. Loaded " + Scripts.getInstance().getClasses().size() + " classes.");
		}
		else if(command.equals("admin_sqreload"))
		{
			String quest = args[0];
			if(Scripts.getInstance().reloadQuest(quest))
				player.sendMessage("Quest '" + quest + "' reloaded with errors.");
			else
				player.sendMessage("Quest '" + quest + "' successfully reloaded.");
			reloadQuestStates(player);
		}
		return true;
	}

	private void reloadQuestStates(L2Player p)
	{
		for(QuestState qs : p.getAllQuestsStates())
			p.delQuestState(qs.getQuest().getName());

		Quest.playerEnter(p);
	}

	@Override
	public AdminCommandDescription[] getAdminCommandList()
	{
		return _adminCommands;
	}
}