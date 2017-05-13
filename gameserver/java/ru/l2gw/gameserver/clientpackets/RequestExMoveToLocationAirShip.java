package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2AirShipDock;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * Format: d d|dd
 */
public class RequestExMoveToLocationAirShip extends L2GameClientPacket
{
	@Override
	protected void runImpl()
	{
	}

	@Override
	protected void readImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || !(player.getVehicle() instanceof L2ClanAirship))
			return;

		if(System.currentTimeMillis() - player.getLastMovePacket() < 1000)
			return;

		player.setLastMovePacket();

		L2ClanAirship airship = (L2ClanAirship) player.getVehicle();
		if(airship.getCaptainObjectId() != player.getObjectId())
			return;

		switch(readD())
		{
			case 4: // AirShipTeleport
				int portId = readD();
				L2AirShipDock dock = airship.getCurrentDock();
				if(dock != null && !airship.isMoving)
				{
					StatsSet port = dock.getPort(portId);
					if(port == null)
						return;
					
					if(airship.getCurrentEp() < port.getInteger("ep"))
					{
						player.sendPacket(Msg.YOUR_SHIP_CANNOT_TELEPORT_BECAUSE_IT_DOES_NOT_HAVE_ENOUGH_FUEL_FOR_THE_TRIP);
						return;
					}
					if(dock.getRouteByType(String.valueOf(portId)) == null)
					{
						System.out.println("No route type: " + String.valueOf(portId));
						return;
					}

					airship.setCurrentRoute(dock.getRouteByType(String.valueOf(portId)));
					airship.setManualControl(false);
					airship.getAI().depart();
				}
				break;
			case 0: // Free move
				if(airship.isManualControlled())
					airship.moveToLocation(airship.getLoc().setX(readD()).setY(readD()), 0, false);
				break;
			case 2: // Up
				if(airship.isManualControlled())
				{
					readD(); //?
					readD(); //?
					airship.moveToLocation(airship.getX(), airship.getY(), Math.min(airship.getZ() + 500, 6000), 0, false);
				}
				break;
			case 3: // Down
				if(airship.isManualControlled())
				{
					readD(); //?
					readD(); //?
					airship.moveToLocation(airship.getX(), airship.getY(), Math.max(airship.getZ() - 500, -1000), 0, false);
				}	
				break;
		}
	}
}