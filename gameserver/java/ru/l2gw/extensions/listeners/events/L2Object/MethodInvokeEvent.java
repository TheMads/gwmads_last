package ru.l2gw.extensions.listeners.events.L2Object;

import ru.l2gw.extensions.listeners.events.DefaultMethodInvokeEvent;
import ru.l2gw.gameserver.model.L2Object;

/**
 * @author Death
 */
public class MethodInvokeEvent extends DefaultMethodInvokeEvent
{
	public MethodInvokeEvent(String methodName, L2Object owner, Object[] args)
	{
		super(methodName, owner, args);
	}

	public L2Object getObject()
	{
		return (L2Object) getOwner();
	}
}
