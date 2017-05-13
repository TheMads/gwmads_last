package ru.l2gw.extensions.listeners.items;

import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 * @author rage
 * @date 18.10.2010 11:41:42
 */
public class UnequipDispelListener extends ItemEquipListener
{
	private final String[] _abnormals;

	public UnequipDispelListener(String list)
	{
		_abnormals = list.split(",");	
	}

	public void onEquip(L2ItemInstance item, L2Playable actor)
	{
	}

	public void onUnEquip(L2ItemInstance item, L2Playable actor)
	{
		actor.stopEffects(_abnormals);
	}
}
