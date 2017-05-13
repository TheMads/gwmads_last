package ru.l2gw.gameserver.model.entity.vehicle.actions;

import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;

/**
 * @author rage
 * @date 07.09.2010 16:25:40
 */
public class manualAction extends StationAction
{
	private boolean _manual;

	@Override
	public void parseAction(Node an) throws Exception
	{
		_manual = Boolean.parseBoolean(an.getAttributes().getNamedItem("control").getNodeValue());

		super.parseAction(an);
	}

	@Override
	public void doAction(L2Vehicle vehicle)
	{
		if(vehicle instanceof L2ClanAirship)
			((L2ClanAirship) vehicle).setManualControl(_manual);
	}
}
