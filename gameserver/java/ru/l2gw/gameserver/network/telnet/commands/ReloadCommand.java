package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.extensions.ccpGuard.Protection;
import ru.l2gw.extensions.ccpGuard.managers.HwidBan;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.ProductManager;
import ru.l2gw.gameserver.model.L2Multisell;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.util.Files;

/**
 * @author: rage
 * @date: 04.03.12 0:11
 */
public class ReloadCommand extends TelnetCommand
{
	public ReloadCommand()
	{
		super("reload");
	}

	@Override
	public String getUsage()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("usage: reload <what>\n");
		sb.append("Available reload commands:\n");
		sb.append("  skills        - reload skill data;\n");
		sb.append("  npc           - reload npc data;\n");
		sb.append("  multisell     - reload multisell data;\n");
		sb.append("  htm           - reload html;\n");
		sb.append("  gmaccess      - reload GM Access profiles;\n");
		sb.append("  scripts       - reload scripts;\n");
		sb.append("  protect       - reload protection config if enabled;\n");
		sb.append("  hwid          - reload HWID bans;\n");
		sb.append("  productdata   - reload productdata.xml;\n");
		sb.append("  config <file> - reload config <file>\n");
		sb.append("  available: server, clanhall, other, spoil, alternative, services, pvp, ai,\n");
		sb.append("             hexid, events, donate, vitality, boss, globalwar, abuse, gmaccess\n");
		return sb.toString();
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(!checkArgs(1, args))
			return null;
		
		StringBuilder sb = new StringBuilder();
		if(args[0].equalsIgnoreCase("skills"))
		{
			SkillTable.getInstance().reload();
			sb.append("Skills reloaded.\n");
		}
		else if(args[0].equalsIgnoreCase("npc"))
		{
			NpcTable.getInstance().reloadAllNpc();
			sb.append("NPC's reloaded.\n");
		}
		else if(args[0].equalsIgnoreCase("multisell"))
		{
			L2Multisell.getInstance().reload();
			sb.append("Multisell reloaded.\n");
		}
		else if(args[0].equalsIgnoreCase("htm"))
		{
			Files.cacheClean();
			sb.append("HTM cache clean.\n");
		}
		else if(args[0].equalsIgnoreCase("gmaccess"))
		{
			Config.loadGMAccess();
			sb.append("GMAccess reloaded.\n");
		}
		else if(args[0].equalsIgnoreCase("scripts"))
		{
			Scripts.getInstance().reload();
			sb.append("Scripts reloaded.\n");
		}
		else if(args[0].equalsIgnoreCase("protect"))
		{
			Protection.Init();
			sb.append("Protection reloaded.\n");
		}
		else if(args[0].equalsIgnoreCase("hwid"))
		{
			HwidBan.reload();
			sb.append("HWID bans reloaded.\n");
		}
		else if(args[0].equalsIgnoreCase("productdata"))
		{
			ProductManager.reloadProductData();
			sb.append("productdata.xml reloaded.\n");
		}
		else if(args[0].equalsIgnoreCase("config"))
		{
			if(args.length > 1 && !args[1].isEmpty())
			{
				Config.reload(args[1]);
				sb.append("Config: ").append(args[1]).append(" reload.\n");
			}
			else
				sb.append("reload config ").append(args.length > 1 ? args[1] : "null").append(" unsupported.\n");
		}
		else
			sb.append("reload " + args[0] + " unsupported.\n");

		return sb.toString();
	}
}