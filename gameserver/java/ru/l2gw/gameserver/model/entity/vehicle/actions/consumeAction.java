package ru.l2gw.gameserver.model.entity.vehicle.actions;

import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;

/**
 * @author rage
 * @date 10.09.2010 15:26:49
 */
public class consumeAction extends StationAction
{
	private int _ep;

	@Override
	public void parseAction(Node an) throws Exception
	{
		_ep = Integer.parseInt(an.getAttributes().getNamedItem("ep").getNodeValue());
		super.parseAction(an);
	}

	@Override
	public void doAction(L2Vehicle vehicle)
	{
		if(vehicle instanceof L2ClanAirship)
		{
			L2ClanAirship cas = (L2ClanAirship) vehicle;
			cas.setCurrentEp(cas.getCurrentEp() - _ep);
			cas.broadcastUserInfo();
		}
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[ep=" + _ep + ";delay=" + getDelay() + "]";
	}
}
