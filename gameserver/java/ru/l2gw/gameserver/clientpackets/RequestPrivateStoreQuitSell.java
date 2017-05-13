package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

public class RequestPrivateStoreQuitSell extends L2GameClientPacket
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
		if(player.getTradeList() != null)
			player.getTradeList().removeAll();
		if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE)
			player.setPrivateStoreManage(false);
		player.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		player.standUp();
		player.broadcastUserInfo(true);
		player.tempWhEnable();
	}
}