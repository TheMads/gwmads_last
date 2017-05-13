package ru.l2gw.gameserver.model.zone;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;

/**
 * Created by IntelliJ IDEA.
 * User: admin
 * Date: 16.02.2009
 * Time: 9:21:55
 */
public class L2CastleTrapZone extends L2DefaultZone
{
	@Override
	public void register()
	{
		// Register self to the correct castle
		ResidenceManager.getInstance().addZone(this);
	}
}
