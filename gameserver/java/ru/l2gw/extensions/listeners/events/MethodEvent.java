package ru.l2gw.extensions.listeners.events;

import ru.l2gw.extensions.listeners.engine.MethodInvocationResult;

/**
 * @author Death
 */
public interface MethodEvent
{
	public Object getOwner();

	public Object[] getArgs();

	public String getMethodName();

	public MethodInvocationResult getInvocationResult();

	public void setInvocationResult(MethodInvocationResult result);
}
