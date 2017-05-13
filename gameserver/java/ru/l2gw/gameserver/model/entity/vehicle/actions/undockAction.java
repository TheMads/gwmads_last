package ru.l2gw.gameserver.model.entity.vehicle.actions;

import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;

/**
 * @author rage
 * @date 10.09.2010 15:06:35
 */
public class undockAction extends StationAction
{
	public void doAction(L2Vehicle vehicle)
	{
		if(vehicle instanceof L2ClanAirship)
		{
			L2ClanAirship cas = (L2ClanAirship) vehicle;
			if(cas.getCurrentDock() != null)
				cas.getCurrentDock().setDockedShip(null);

			cas.setCurrentDock(null);
		}
	}
}
