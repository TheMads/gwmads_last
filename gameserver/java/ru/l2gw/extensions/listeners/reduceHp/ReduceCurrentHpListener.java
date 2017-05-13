package ru.l2gw.extensions.listeners.reduceHp;

import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.listeners.MethodInvokeListener;
import ru.l2gw.extensions.listeners.events.MethodEvent;
import ru.l2gw.gameserver.model.L2Character;

/**
 * User: Death
 */
public abstract class ReduceCurrentHpListener implements MethodInvokeListener
{
	@Override
	public final void methodInvoked(MethodEvent e)
	{
		Object[] args = e.getArgs();
		onReduceCurrentHp((L2Character) e.getOwner(), (Double) args[0], (L2Character) args[1], (Boolean) args[2], e);
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
		return event.getMethodName().equals(MethodCollection.ReduceCurrentHp);
	}

	public abstract void onReduceCurrentHp(L2Character actor, double damage, L2Character attacker, boolean directHp, MethodEvent event);
}
