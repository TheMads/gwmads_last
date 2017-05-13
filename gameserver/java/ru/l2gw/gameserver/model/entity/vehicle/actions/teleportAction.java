package ru.l2gw.gameserver.model.entity.vehicle.actions;

import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

/**
 * @author: rage
 * @date: 24.06.2010 21:16:50
 */
public class teleportAction extends StationAction
{
	private Location _point;

	public void parseAction(Node an) throws Exception
	{
		int x = Integer.parseInt(an.getAttributes().getNamedItem("x").getNodeValue());
		int y = Integer.parseInt(an.getAttributes().getNamedItem("y").getNodeValue());
		int z = Integer.parseInt(an.getAttributes().getNamedItem("z").getNodeValue());
		int h = an.getAttributes().getNamedItem("h") != null ? Integer.parseInt(an.getAttributes().getNamedItem("h").getNodeValue()) : -1;
		_point = new Location(x, y, z, h);

		super.parseAction(an);
	}

	public void doAction(L2Vehicle vehicle)
	{
		if(_point.getHeading() > -1)
			vehicle.setHeading(_point.getHeading());
		vehicle.teleToLocation(_point);
	}
}
