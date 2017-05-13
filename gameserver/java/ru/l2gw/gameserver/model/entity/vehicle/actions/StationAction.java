package ru.l2gw.gameserver.model.entity.vehicle.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;

/**
 * @author rage
 * @date 07.05.2010 10:08:25
 */
public abstract class StationAction
{
	private long _delay;
	protected static final Log _log = LogFactory.getLog("vehicle");
	
	public void setDelay(long delay)
	{
		_delay = delay;
	}

	public long getDelay()
	{
		return _delay;
	}

	public void parseAction(Node an) throws Exception
	{
		Node attr = an.getAttributes().getNamedItem("delay");
		_delay = attr == null ? 0 : Long.parseLong(attr.getNodeValue());
	}

	public abstract void doAction(L2Vehicle vehicle);

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}
}
