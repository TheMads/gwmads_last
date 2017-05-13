package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.ProductManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExBR_ProductList;

public class RequestExBR_ProductList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(Config.PRODUCT_SHOP_ENABLED)
		{
			player.sendPacket(new ExBR_ProductList(ProductManager.getProductList()));
		}
		else
		{
			player.sendPacket(new ExBR_ProductList(ProductManager.emptyData));
		}
	}
}