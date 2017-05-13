package ru.l2gw.gameserver.model.entity.vehicle.actions;

import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.serverpackets.PlaySound;

/**
 * @author rage
 * @date 07.05.2010 10:28:52
 */
public class playAction extends StationAction
{
	private String _sound;

	public void parseAction(Node an) throws Exception
	{
		_sound = an.getAttributes().getNamedItem("sound").getNodeValue();
		super.parseAction(an);
	}

	public void doAction(L2Vehicle vehicle)
	{
		vehicle.broadcastPacketToPoints(new PlaySound(vehicle, _sound));
	}
}
