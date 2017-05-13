package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.handler.AdminCommandHandler;
import ru.l2gw.gameserver.model.L2Player;

/**
 * This class handles all GM commands triggered by //command
 */
public class SendBypassBuildCmd extends L2GameClientPacket
{
	// format: cS
	private static final Log logCmd = LogFactory.getLog("admincmd");
	private String _command;

	@Override
	public void readImpl()
	{
		_command = readS();

		if(_command != null)
			_command = _command.trim();

	}

	@Override
	public void runImpl()
	{
		if(Config.DEBUG)
			_log.info("Got command '" + _command + "'");

		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		AdminCommandHandler.getInstance().useAdminCommandHandler(player, "admin_" + _command);
	}
}