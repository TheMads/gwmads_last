package ru.l2gw.gameserver.pservercon.pspackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.ProductManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.pservercon.PSClient;
import ru.l2gw.gameserver.serverpackets.ExBR_GamePoint;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * @author: rage
 * @date: 16.10.11 1:26
 */
public class ResponseGamePoint extends PSBasePacket
{
	public ResponseGamePoint(byte[] data, PSClient client)
	{
		super(data, client);
	}

	@Override
	public void read()
	{
		int jobId = readD();
		readD();
		readD();
		readH();
		readH();
		long points = readQ();

		if(Config.PRODUCT_SERVER_DEBUG)
			_log.info("PSConnection: ResponseGamePoint job_id: " + jobId + " points: " + points);

		StatsSet job = ProductManager.removeJob(jobId);
		L2Player player = L2ObjectsStorage.getPlayer(job.getInteger("object_id"));
		if(player != null)
			player.sendPacket(new ExBR_GamePoint(job.getInteger("object_id"), points));
	}
}