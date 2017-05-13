package ru.l2gw.extensions.listeners;

import ru.l2gw.extensions.listeners.events.MethodEvent;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;

/**
 * @author: rage
 * @date: 18.09.11 0:24
 */
public abstract class DoorOpenCloseListener implements MethodInvokeListener
{
	@Override
	public final void methodInvoked(MethodEvent e)
	{
		onOpenClose((L2DoorInstance) e.getOwner(), (Integer) e.getArgs()[0]);
	}

	/**
	 * Простенький фильтр. Фильтрирует по названии метода и аргументам.
	 * Ничто не мешает переделать при нужде :)
	 *
	 * @param event событие с аргументами
	 *
	 * @return true если все ок ;)
	 */
	@Override
	public final boolean accept(MethodEvent event)
	{
		return event.getMethodName().equals(MethodCollection.onDoorOpenClose);
	}

	public abstract void onOpenClose(L2DoorInstance door, int open);
}
