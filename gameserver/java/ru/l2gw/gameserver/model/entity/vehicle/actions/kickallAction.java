package ru.l2gw.gameserver.model.entity.vehicle.actions;

import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;

/**
 * @author: rage
 * @date: 24.06.2010 21:24:49
 */
public class kickallAction extends kickAction
{
	@Override
	public void doAction(L2Vehicle vehicle)
	{
		if(vehicle.getOnBoardPlayer() != null)
			for(int objectId : vehicle.getOnBoardPlayer())
			{
				L2Player player = L2ObjectsStorage.getPlayer(objectId);
				if(player != null && player.getVehicle() == vehicle)
					vehicle.oustPlayer(player, _point);
			}
	}
}
