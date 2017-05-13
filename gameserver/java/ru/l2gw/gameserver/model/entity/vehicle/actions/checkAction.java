package ru.l2gw.gameserver.model.entity.vehicle.actions;

import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.serverpackets.VehicleStarted;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 07.05.2010 10:36:28
 */
public class checkAction extends StationAction
{
	private int _ticketId;
	private Location _point;

	@Override
	public void parseAction(Node an) throws Exception
	{
		_ticketId = Integer.parseInt(an.getAttributes().getNamedItem("ticketId").getNodeValue());
		int x = Integer.parseInt(an.getAttributes().getNamedItem("x").getNodeValue());
		int y = Integer.parseInt(an.getAttributes().getNamedItem("y").getNodeValue());
		int z = Integer.parseInt(an.getAttributes().getNamedItem("z").getNodeValue());

		_point = new Location(x, y, z);

		super.parseAction(an);
	}

	public void doAction(L2Vehicle vehicle)
	{
		if(vehicle.getOnBoardPlayer() != null)
		{
			GArray<L2Player> expel = new GArray<L2Player>();
			for(int objectId : vehicle.getOnBoardPlayer())
			{
				L2Player player = L2ObjectsStorage.getPlayer(objectId);
				if(player != null && player.getVehicle() == vehicle)
				{
					if(!player.destroyItemByItemId("Vehicle", _ticketId, 1, vehicle, false))
						expel.add(player);
					else
						player.sendPacket(new VehicleStarted(vehicle, 1));
				}
			}

			for(L2Player player : expel)
				player.teleToLocation(_point);
		}
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[ticket=" + _ticketId + ";x=" + _point.getX() + ";y=" + _point.getY() + ";z=" + _point.getZ() + "]";
	}
}
