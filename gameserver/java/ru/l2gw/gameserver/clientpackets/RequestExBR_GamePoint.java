package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.ProductManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.pservercon.PSConnection;
import ru.l2gw.gameserver.pservercon.gspackets.RequestGamePoints;
import ru.l2gw.gameserver.templates.StatsSet;

public class RequestExBR_GamePoint extends L2GameClientPacket
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

			StatsSet job = ProductManager.addJobForObjectId(player.getObjectId());
			PSConnection.getInstance().sendPacket(new RequestGamePoints(job.getInteger("job_id"), getClient().getAccountId()));
		}
	}
}