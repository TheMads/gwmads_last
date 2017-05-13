package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.instancemanager.ProductManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.ProductData;
import ru.l2gw.gameserver.pservercon.PSConnection;
import ru.l2gw.gameserver.pservercon.gspackets.*;
import ru.l2gw.gameserver.serverpackets.ExBR_BuyProduct;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.StatsSet;

public class RequestExBR_BuyProduct extends L2GameClientPacket
{
	private static final Log log = LogFactory.getLog("product");
	private int productId;
	private long amount;

	@Override
	public void readImpl()
	{
		productId = readD();
		amount = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(amount <= 0 || amount > Integer.MAX_VALUE)
		{
			log.warn("RequestExBR_BuyProduct: " + getClient() + " amount error: " + amount);
			sendPacket(new ExBR_BuyProduct(ExBR_BuyProduct.RESULT_WRONG_PRODUCT_ITEM));
			return;
		}

		ProductData pd = ProductManager.getProductById(productId);
		if(pd == null || pd.buyable <= 0)
		{
			log.warn("RequestExBR_BuyProduct: " + getClient() + " wrong product: " + productId);
			sendPacket(new ExBR_BuyProduct(ExBR_BuyProduct.RESULT_WRONG_PRODUCT));
			return;
		}

		if(pd.sale_start_date > System.currentTimeMillis() / 1000 || pd.sale_end_date < System.currentTimeMillis() / 1000)
		{
			log.warn("RequestExBR_BuyProduct: " + getClient() + " wrong sale time: " + productId);
			sendPacket(new ExBR_BuyProduct(ExBR_BuyProduct.RESULT_SALE_PERIOD_ENDED));
			return;
		}

		int slots = 0;
		long weight = 0;
		for(StatsSet ii : pd.items)
		{
			L2Item item = ItemTable.getInstance().getTemplate(ii.getInteger("item_id"));
			if(item == null)
			{
				sendPacket(new ExBR_BuyProduct(ExBR_BuyProduct.RESULT_WRONG_PRODUCT_ITEM));
				return;
			}

			if(!item.isStackable())
			{
				slots += ii.getInteger("item_count") * amount;
				weight += item.getWeight() * amount;
			}
			else if(player.getItemCountByItemId(item.getItemId()) < 1)
			{
				slots++;
				weight += item.getWeight();
			}
		}

		slots += player.getInventoryItemsCount();
		if(!player.getInventory().validateCapacity(slots) || !player.getInventory().validateWeight(weight))
		{
			sendPacket(new ExBR_BuyProduct(ExBR_BuyProduct.RESULT_INVENTORY_FULL));
			return;
		}

		StatsSet job = ProductManager.addJobForObjectId(player.getObjectId());
		job.set("product_id", productId);
		job.set("amount", amount);
		PSConnection.getInstance().sendPacket(new RequestBuyProductItem(job.getInteger("job_id"), getClient().getAccountId(), player.getObjectId(), productId, (int) amount, pd.price, player.getName()));
	}
}