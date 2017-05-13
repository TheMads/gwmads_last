package ru.l2gw.gameserver.model.inventory.listeners;

import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.base.EnchantOption;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.FuncAdd;

public final class StatsListener implements PaperdollListener
{
	Inventory _inv;

	public StatsListener(Inventory inv)
	{
		_inv = inv;
	}

	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		if(slot == Inventory.PAPERDOLL_LRHAND || slot < 0)
			return;
		_inv.getOwner().removeStatsOwner(item);
	}

	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		if(slot == Inventory.PAPERDOLL_LRHAND || slot < 0)
			return;

		L2Character owner = _inv.getOwner();
		owner.addStatFuncs(item.getStatFuncs(owner));

		if(item.getEnchantHpBonus() > 0)
			owner.addStatFunc(new FuncAdd(Stats.MAX_HP, 0x50, item, item.getEnchantHpBonus()));
	}
}
