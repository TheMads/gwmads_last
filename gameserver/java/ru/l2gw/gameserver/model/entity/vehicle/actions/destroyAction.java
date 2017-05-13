package ru.l2gw.gameserver.model.entity.vehicle.actions;

import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;

/**
 * @author rage
 * @date 07.09.2010 16:32:02
 */
public class destroyAction extends StationAction
{
	@Override
	public void doAction(L2Vehicle vehicle)
	{
		if(vehicle instanceof L2ClanAirship)
			((L2ClanAirship) vehicle).deleteMe();
	}
}
