package ru.l2gw.extensions.listeners.items;

import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 * @author rage
 * @date 18.10.2010 11:18:10
 */
public abstract class ItemEquipListener
{
	public abstract void onEquip(L2ItemInstance item, L2Playable actor);

	public abstract void onUnEquip(L2ItemInstance item, L2Playable actor);
}
