package ru.l2gw.gameserver.model.inventory.listeners;

import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.templates.L2Weapon.WeaponType;

public final class BowListener implements PaperdollListener
{
	Inventory _inv;

	public BowListener(Inventory inv)
	{
		_inv = inv;
	}

	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		if(slot != Inventory.PAPERDOLL_RHAND)
			return;
		if(item.getItemType() == WeaponType.BOW)
			_inv.setPaperdollItem(Inventory.PAPERDOLL_LHAND, null);
		if(item.getItemType() == WeaponType.CROSSBOW)
			_inv.setPaperdollItem(Inventory.PAPERDOLL_LHAND, null);
		if(item.getItemType() == WeaponType.ROD && !_inv.isRefreshingListeners())
			_inv.setPaperdollItem(Inventory.PAPERDOLL_LHAND, null);
	}

	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		if(slot != Inventory.PAPERDOLL_RHAND)
			return;
		if(item.getItemType() == WeaponType.BOW)
		{
			L2ItemInstance arrow = _inv.findArrowForBow(item.getItem());
			if(arrow != null)
				_inv.setPaperdollItem(Inventory.PAPERDOLL_LHAND, arrow);
		}
		if(item.getItemType() == WeaponType.CROSSBOW)
		{
			L2ItemInstance bolt = _inv.findArrowForCrossbow(item.getItem());
			if(bolt != null)
				_inv.setPaperdollItem(Inventory.PAPERDOLL_LHAND, bolt);
		}
	}
}
