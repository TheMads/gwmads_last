package ru.l2gw.extensions.ccpGuard;

import ru.l2gw.extensions.ccpGuard.managers.HwidBan;
import ru.l2gw.extensions.ccpGuard.managers.HwidInfo;
import ru.l2gw.extensions.ccpGuard.managers.HwidManager;
import ru.l2gw.extensions.ccpGuard.managers.ProtectManager;
import ru.l2gw.extensions.scripts.Functions;
import ru.l2gw.gameserver.handler.AdminCommandDescription;
import ru.l2gw.gameserver.handler.IAdminCommandHandler;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;

import java.util.StringTokenizer;

public class AdminHWID implements IAdminCommandHandler
{
	private static AdminCommandDescription[] _adminCommands = {
			new AdminCommandDescription("admin_hwid_ban", null),
			new AdminCommandDescription("admin_hwid_reload", null),
			new AdminCommandDescription("admin_hwid_count", null),
			new AdminCommandDescription("admin_hwid_names", null),
			new AdminCommandDescription("admin_hwid_windows", null),
			new AdminCommandDescription("admin_hwid_lock_account", null),
			new AdminCommandDescription("admin_hwid_lock_player", null) };

	@Override
	public boolean useAdminCommand(String command, String[] args, String fullCommand, L2Player player)
	{
		if(!ConfigProtect.PROTECT_ENABLE)
			return false;
		if(player == null)
			return false;
		
		if(!AdminTemplateManager.checkCommand(command, player, null, null, null, null))
		{
			Functions.sendSysMessage(player, "Access deined.");
			return false;
		}
		
		if(!command.startsWith("admin_hwid"))
			return false;
		if(command.startsWith("admin_hwid_ban"))
		{

			L2Object plTarget = player.getTarget();
			if(plTarget == null)
			{
				player.sendMessage("Target is empty");
				return false;
			}
			L2Player target = plTarget.getPlayer();
			if(target != null)
			{
				if(command.length() > 15)
					command = command.substring(15);
				else
					command = "no comment";
				HwidBan.addHwidBan(target.getNetConnection(), command + " [" + player.getName() + "]");
				player.sendMessage(target.getName() + " banned in HWID");
			}
			else
				player.sendMessage("Target is not player");
		}
		else if(command.startsWith("admin_hwid_reload"))
		{
			HwidBan.reload();
			player.sendMessage("HWID reload, " + HwidBan.getCountHwidBan() + " bans");
			HwidManager.reload();
			player.sendMessage("HwidManager reload, " + HwidManager.getCountHwidInfo() + " hwids");
		}
		else if(command.startsWith("admin_hwid_count"))
		{
			L2Player target = player.getTarget().getPlayer();
			if(target != null)
			{
				int count = ProtectManager.getInstance().getCountByHWID(target.getNetConnection()._prot_info.getHWID());
				player.sendMessage(target.getName() + " has " + count + " connections opened.");
			}
			else
				player.sendMessage("Target is not player");
		}
		else if(command.startsWith("admin_hwid_names"))
		{
			L2Player target = player.getTarget().getPlayer();
			if(target != null)
			{
				player.sendMessage("Here all character's names by targeted character HWID:");
				for(String name : ProtectManager.getInstance().getNamesByHWID(target.getNetConnection()._prot_info.getHWID()))
				{
					player.sendMessage(name);
				}
			}
			else
				player.sendMessage("Target is not player");
		}
		else if(command.startsWith("admin_hwid_windows"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command);
				if(st.countTokens() > 1)
				{
					st.nextToken();
					String countStr = st.nextToken();
					int windowsCount = Integer.parseInt(countStr);
					L2Player target = null;
					if(player.getTarget() != null)
					{
						target = player.getTarget().getPlayer();
					}
					else
					{
						player.sendMessage("Target is not player");
					}
					if(target != null)
					{
						HwidManager.updateHwidInfo(target, windowsCount);
						player.sendMessage(target.getName() + " set " + windowsCount + " allowed windows.");
					}
					else
					{
						player.sendMessage("Target is not player");
					}
				}
			}
			catch(StringIndexOutOfBoundsException e)
			{
				player.sendMessage("Please specify new allowed windows count value.");
			}
		}
		else if(command.startsWith("admin_hwid_lock_account"))
		{
			if(!ConfigProtect.PROTECT_ENABLE_HWID_LOCK)
				return false;
			L2Player target = player.getTarget().getPlayer();
			if(target != null)
			{
				HwidManager.updateHwidInfo(player, HwidInfo.LockType.ACCOUNT_LOCK);
				player.sendMessage(target.getName() + " was locked (account lock)");
			}
			else
			{
				player.sendMessage("Target is not player");
			}
		}
		else if(command.startsWith("admin_hwid_lock_player"))
		{
			if(!ConfigProtect.PROTECT_ENABLE_HWID_LOCK)
				return false;
			L2Player target = player.getTarget().getPlayer();
			if(target != null)
			{
				HwidManager.updateHwidInfo(player, HwidInfo.LockType.PLAYER_LOCK);
				player.sendMessage(target.getName() + " was locked (account lock)");
			}
			else
			{
				player.sendMessage("Target is not player");
			}
		}
		return true;
	}

	@Override
	public AdminCommandDescription[] getAdminCommandList()
	{
		return _adminCommands;
	}
}