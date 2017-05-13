package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.ProductManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExBR_RecentProductList;

public class RequestExBR_RecentProductList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{
	}

	@Override
	public void runImpl()
	{
		if(Config.PRODUCT_SHOP_ENABLED)
		{
			L2Player player = getClient().getPlayer();
			if(player == null)
				return;

			player.sendPacket(new ExBR_RecentProductList(ProductManager.getBuyHistory(player.getObjectId())));
		}
	}
}