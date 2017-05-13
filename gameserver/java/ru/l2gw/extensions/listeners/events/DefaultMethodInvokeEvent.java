package ru.l2gw.extensions.listeners.events;

import ru.l2gw.extensions.listeners.engine.MethodInvocationResult;

/**
 * @author Death
 */
public class DefaultMethodInvokeEvent implements MethodEvent
{
	private final Object owner;
	private final Object[] args;
	private final String methodName;
	private MethodInvocationResult invocationResult;

	public DefaultMethodInvokeEvent(String methodName, Object owner, Object[] args)
	{
		this.methodName = methodName;
		this.owner = owner;
		this.args = args;
		invocationResult = MethodInvocationResult.CONTINUE;
	}

	@Override
	public Object getOwner()
	{
		return owner;
	}

	@Override
	public Object[] getArgs()
	{
		return args;
	}

	@Override
	public String getMethodName()
	{
		return methodName;
	}

	@Override
	public MethodInvocationResult getInvocationResult()
	{
		return invocationResult;
	}

	@Override
	public void setInvocationResult(MethodInvocationResult result)
	{
		invocationResult = result;
	}
}