package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SocialAction;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.util.Util;

public class RequestSocialAction extends L2GameClientPacket
{
	private int _actionId;

	/**
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		_actionId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isOutOfControl() || (player.getTransformation() != 0 && player.getSkillLevel(838) < 1))
		{
			player.sendActionFailed();
			return;
		}

		// You cannot do anything else while fishing
		if(player.isFishing())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DO_ANYTHING_ELSE_WHILE_FISHING));
			return;
		}

		// internal Social Action check
		if(_actionId < 2 || _actionId > 14)
		{
			Util.handleIllegalPlayerAction(player, "RequestSocialAction[43]", "Character " + player.getName() + " at account " + player.getAccountName() + "requested an internal Social Action " + _actionId, 1);
			return;
		}

		if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE && player.getTransactionRequester() == null && !player.isActionsDisabled() && !player.isSitting())
		{
			player.broadcastPacket(new SocialAction(player.getObjectId(), _actionId));

			if(Config.ALT_SOCIAL_ACTION_REUSE)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new SocialTask(player), 2600);
				player.block();
			}
		}
	}

	class SocialTask implements Runnable
	{
		L2Player _player;

		SocialTask(L2Player player)
		{
			_player = player;
		}

		public void run()
		{
			_player.unblock();
		}
	}
}