package ru.l2gw.gameserver.model.inventory.listeners;

import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public final class ItemAugmentationListener implements PaperdollListener
{
	Inventory _inv;

	public ItemAugmentationListener(Inventory inv)
	{
		_inv = inv;
	}

	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		L2Player player;

		if(_inv.getOwner().isPlayer())
			player = (L2Player) _inv.getOwner();
		else
			return;

		if(item.isAugmented())
			item.getAugmentation().removeBonus(player);
	}

	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		L2Player player;

		if(_inv.getOwner().isPlayer())
			player = (L2Player) _inv.getOwner();
		else
			return;

		if(item.isAugmented())
			item.getAugmentation().applyBonus(player);
	}
}