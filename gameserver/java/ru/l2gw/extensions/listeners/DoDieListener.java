package ru.l2gw.extensions.listeners;

import ru.l2gw.extensions.listeners.events.MethodEvent;
import ru.l2gw.gameserver.model.L2Character;

/**
 * User: Death
 */
public abstract class DoDieListener implements MethodInvokeListener
{
	@Override
	public final void methodInvoked(MethodEvent e)
	{
		onDie((L2Character) e.getOwner());
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
		return event.getMethodName().equals(MethodCollection.doDie);
	}

	public abstract void onDie(L2Character cha);
}
