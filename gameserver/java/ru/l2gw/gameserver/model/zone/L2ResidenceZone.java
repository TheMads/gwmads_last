package ru.l2gw.gameserver.model.zone;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;

public class L2ResidenceZone extends L2DefaultZone
{
	public L2ResidenceZone()
	{
		super();
	}

	@Override
	public void register()
	{
		// Register self to the correct castle
		ResidenceManager.getInstance().addZone(this);
	}

}
