package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.ProductManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.ProductData;
import ru.l2gw.gameserver.serverpackets.ExBR_ProductInfo;

public class RequestExBR_ProductInfo extends L2GameClientPacket
{
	private int productId;

	@Override
	public void readImpl()
	{
		productId = readD();
	}

	@Override
	public void runImpl()
	{
		if(Config.PRODUCT_SHOP_ENABLED)
		{
			L2Player player = getClient().getPlayer();
			if(player == null)
				return;

			ProductData pd = ProductManager.getProductById(productId);
			if(pd != null)
				player.sendPacket(new ExBR_ProductInfo(pd));
		}
	}
}