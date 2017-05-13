package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class Logout extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		// Dont allow leaving if player is fighting
		if(player.isInCombat())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_LOGOUT_WHILE_IN_COMBAT));
			player.sendActionFailed();
			return;
		}

		if(player.isFishing())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DO_ANYTHING_ELSE_WHILE_FISHING));
			player.sendActionFailed();
			return;
		}

		if(player.isBlocked())
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.Logout.OutOfControl", player));
			player.sendActionFailed();
			return;
		}

		if(!Config.ALT_OLY_ALLOW_CLIENT_RESTART && player.isInOlympiadMode())
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.Logout.Olympiad", player));
			player.sendActionFailed();
			return;
		}

		if(player.inObserverMode())
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.Logout.Observer", player));
			player.sendActionFailed();
			return;
		}

		player.logout(false, false, false);
	}
}