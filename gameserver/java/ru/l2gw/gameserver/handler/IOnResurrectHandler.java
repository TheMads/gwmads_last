package ru.l2gw.gameserver.handler;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author: rage
 * @date: 21.06.12 22:28
 */
public interface IOnResurrectHandler
{
	public void onResurrected(L2Player player, long reviverStoredId);
}
