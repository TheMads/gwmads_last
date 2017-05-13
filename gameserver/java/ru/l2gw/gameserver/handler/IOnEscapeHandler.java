package ru.l2gw.gameserver.handler;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

/**
 * @author: rage
 * @date: 21.06.12 22:29
 */
public interface IOnEscapeHandler
{
	public Location onEscape(L2Player player);
}
