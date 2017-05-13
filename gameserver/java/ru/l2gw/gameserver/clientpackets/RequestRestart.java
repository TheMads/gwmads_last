package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.network.GameClient.GameClientState;
import ru.l2gw.gameserver.serverpackets.CharacterSelectionInfo;
import ru.l2gw.gameserver.serverpackets.RestartResponse;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestRestart extends L2GameClientPacket
{
	/**
	 * format:      c
	 */
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		if(!Config.ALT_OLY_ALLOW_CLIENT_RESTART && player.isInOlympiadMode())
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestRestart.Olympiad", player));
			sendPacket(RestartResponse.FAIL);
			player.sendActionFailed();
			return;
		}

		if(player.inObserverMode())
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestRestart.Observer", player));
			sendPacket(RestartResponse.FAIL);
			player.sendActionFailed();
			return;
		}

		if(player.isInCombat())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_RESTART_WHILE_IN_COMBAT));
			sendPacket(RestartResponse.FAIL);
			player.sendActionFailed();
			return;
		}

		if(player.isFishing())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DO_ANYTHING_ELSE_WHILE_FISHING));
			sendPacket(RestartResponse.FAIL);
			player.sendActionFailed();
			return;
		}

		if(player.isBlocked())
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestRestart.OutOfControl", player));
			sendPacket(RestartResponse.FAIL);
			player.sendActionFailed();
			return;
		}

		if(getClient() != null)
			getClient().setState(GameClientState.AUTHED);
		player.logout(false, true, false);
		sendPacket(RestartResponse.OK);
		// send char list
		CharacterSelectionInfo cl = new CharacterSelectionInfo(getClient().getLoginName(), getClient().getSessionId().playOkID1);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}
}
