package ru.l2gw.gameserver.model.inventory.listeners;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 * @author: rage
 * @date: 16.01.13 14:52
 */
public interface InventoryListener
{
	public void itemAdded(L2ItemInstance item);

	public void itemRemoved(L2ItemInstance item);
}
