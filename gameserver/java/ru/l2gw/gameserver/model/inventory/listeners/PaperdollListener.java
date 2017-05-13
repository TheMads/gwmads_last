package ru.l2gw.gameserver.model.inventory.listeners;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public interface PaperdollListener
{
	public void notifyEquipped(int slot, L2ItemInstance inst);

	public void notifyUnequipped(int slot, L2ItemInstance inst);
}
