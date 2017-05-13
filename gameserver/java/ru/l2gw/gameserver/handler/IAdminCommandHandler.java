package ru.l2gw.gameserver.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.L2Player;

public interface IAdminCommandHandler
{
	public static final Log logGM = LogFactory.getLog("gm-actions");

	public boolean useAdminCommand(String command, String[] args, String fullCommand, L2Player player);

	public AdminCommandDescription[] getAdminCommandList();
}
