package ru.l2gw.gameserver.pservercon.pspackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.ProductManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.pservercon.PSClient;
import ru.l2gw.gameserver.pservercon.PSConnection;
import ru.l2gw.gameserver.pservercon.gspackets.DeleteProductItems;
import ru.l2gw.gameserver.serverpackets.ExBR_BuyProduct;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * @author: rage
 * @date: 17.10.11 11:31
 */
public class ResponseBuyProductItem extends PSBasePacket
{
	public ResponseBuyProductItem(byte[] data, PSClient client)
	{
		super(data, client);
	}

	@Override
	public void read()
	{
		int jobId = readD();
		int result = readD();
		long transaction = readQ();
		StatsSet job = ProductManager.getJob(jobId);

		if(Config.PRODUCT_SERVER_DEBUG)
			_log.info("PSConnection: ResponseBuyProductItem job_id: " + jobId + " transaction: " + transaction + " result: " + result + " job is " + (job == null ? "null" : "not null"));

		if(job != null)
		{
			job.set("transaction", transaction);
			if(result > 0)
			{
				L2Player player = L2ObjectsStorage.getPlayer(job.getInteger("object_id"));
				if(player != null)
					player.sendPacket(new ExBR_BuyProduct(ExBR_BuyProduct.RESULT_NOT_ENOUGH_POINTS));
				return;
			}

			if(ProductManager.giveProduct(job, transaction))
				PSConnection.getInstance().sendPacket(new DeleteProductItems(transaction));
		}
	}
}
