package ru.l2gw.gameserver.pservercon.pspackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.ProductManager;
import ru.l2gw.gameserver.pservercon.PSClient;
import ru.l2gw.gameserver.pservercon.PSConnection;
import ru.l2gw.gameserver.pservercon.gspackets.DeleteProductItems;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * @author: rage
 * @date: 17.10.11 13:02
 */
public class ResponseDeleteItems extends PSBasePacket
{
	public ResponseDeleteItems(byte[] data, PSClient client)
	{
		super(data, client);
	}

	@Override
	public void read()
	{
		int jobId = readD();
		int result = readD();

		if(Config.PRODUCT_SERVER_DEBUG)
			_log.info("PSConnection: ResponseDeleteItems job_id: " + jobId + " result: " + result);

		if(result > 0)
		{
			StatsSet job = ProductManager.getJob(jobId);
			if(job != null)
				PSConnection.getInstance().sendPacket(new DeleteProductItems(job.getInteger("transaction")));
		}
		else
			ProductManager.removeJob(jobId);
	}
}