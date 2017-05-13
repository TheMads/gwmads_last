package ru.l2gw.extensions.listeners;

import ru.l2gw.extensions.listeners.events.AbstractAI.AbstractAISetIntention;
import ru.l2gw.extensions.listeners.events.MethodEvent;
import ru.l2gw.gameserver.ai.AbstractAI;
import ru.l2gw.gameserver.ai.CtrlIntention;

/**
 * @Author: Diamond
 * @Date: 08/11/2007
 * @Time: 7:17:24
 */
public abstract class AbstractAISetIntentionListener implements MethodInvokeListener, MethodCollection
{
	@Override
	public final void methodInvoked(MethodEvent e)
	{
		AbstractAISetIntention event = (AbstractAISetIntention) e;
		AbstractAI ai = event.getOwner();
		CtrlIntention evt = (CtrlIntention) event.getArgs()[0];
		Object arg0 = event.getArgs()[1];
		Object arg1 = event.getArgs()[2];
		SetIntention(ai, evt, arg0, arg1);
	}

	@Override
	public final boolean accept(MethodEvent event)
	{
		String method = event.getMethodName();
		return event instanceof AbstractAISetIntention && method.equals(AbstractAIsetIntention);
	}

	public abstract void SetIntention(AbstractAI ai, CtrlIntention intention, Object arg0, Object arg1);
}
