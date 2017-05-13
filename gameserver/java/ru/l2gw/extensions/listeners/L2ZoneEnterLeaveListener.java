package ru.l2gw.extensions.listeners;

import ru.l2gw.extensions.listeners.events.L2Zone.L2ZoneEnterLeaveEvent;
import ru.l2gw.extensions.listeners.events.MethodEvent;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;

/**
 * @Author: Death
 * @Date: 18/9/2007
 * @Time: 9:34:56
 */
public abstract class L2ZoneEnterLeaveListener implements MethodInvokeListener, MethodCollection
{
	/**
	 * Вызывается при входе/выходе из зоны. После необходимых операций вызывает нужный
	 * абстрактный метод.
	 * @param e объект класса L2ZoneEnterLeaveEvent
	 * @see l2p.extensions.listeners.events.L2Zone.L2ZoneEnterLeaveEvent
	 * @see l2p.gameserver.model.L2Zone#doEnter(l2p.gameserver.model.L2Object)
	 * @see l2p.gameserver.model.L2Zone#doLeave(l2p.gameserver.model.L2Object, boolean)
	 */
	@Override
	public final void methodInvoked(MethodEvent e)
	{
		L2ZoneEnterLeaveEvent event = (L2ZoneEnterLeaveEvent) e;
		L2Zone owner = event.getOwner();
		L2Character actor = (L2Character) event.getArgs()[0];

		if(e.getMethodName().equals(L2ZoneChanged))
		    sendZoneStatus(owner, (L2Player) actor);
		else if(e.getMethodName().equals(L2ZoneObjectEnter))
			objectEntered(owner, actor);
		else
			objectLeaved(owner, actor);
	}

	@Override
	public final boolean accept(MethodEvent event)
	{
		String method = event.getMethodName();
		return event instanceof L2ZoneEnterLeaveEvent && (method.equals(L2ZoneObjectEnter) || method.equals(L2ZoneObjectLeave) || method.equals(L2ZoneChanged));
	}

	/**
	 * Метод вызывается когда объект входит в зону
	 * @param zone зона в которую вошли
	 * @param object вошедший объект
	 * @see l2p.gameserver.model.L2Zone#doEnter(l2p.gameserver.model.L2Object)
	 */
	public abstract void objectEntered(L2Zone zone, L2Character object);

	/**
	 * Метод вызывается когда объект выходит с зоны
	 * @param zone зона с которой вышли
	 * @param object вышедший объект
	 * @see l2p.gameserver.model.L2Zone#doLeave(l2p.gameserver.model.L2Object, boolean)
	 */
	public abstract void objectLeaved(L2Zone zone, L2Character object);

	public abstract void sendZoneStatus(L2Zone zone, L2Player player);
}
