package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

public class RequestRecipeShopManageQuit extends L2GameClientPacket
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

		if(player.isInDuel())
		{
			player.sendActionFailed();
			return;
		}

		player.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		player.broadcastUserInfo(true);
		player.standUp();
	}
}