package ru.l2gw.gameserver.model.entity.vehicle.actions;

import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 07.05.2010 10:18:03
 */
public class kickAction extends StationAction
{
	protected Location _point;

	@Override
	public void parseAction(Node an) throws Exception
	{
		int x = Integer.parseInt(an.getAttributes().getNamedItem("x").getNodeValue());
		int y = Integer.parseInt(an.getAttributes().getNamedItem("y").getNodeValue());
		int z = Integer.parseInt(an.getAttributes().getNamedItem("z").getNodeValue());

		_point = new Location(x, y, z);

		super.parseAction(an);
	}

	public void doAction(L2Vehicle vehicle)
	{
		vehicle.setKickPoint(_point);
	}
}
