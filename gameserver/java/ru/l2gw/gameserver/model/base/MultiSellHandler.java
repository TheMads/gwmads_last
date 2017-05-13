package ru.l2gw.gameserver.model.base;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.base.MultiSellListContainer;

/**
 * @author admin
 * @date 10.12.2010 16:14:03
 */
public interface MultiSellHandler
{
	public int[] getMultiSellId();

	public MultiSellListContainer generateMultiSellList(int listId, L2Player player, double tax);
}
