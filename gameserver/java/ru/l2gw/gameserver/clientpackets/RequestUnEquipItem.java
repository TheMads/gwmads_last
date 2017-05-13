package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.templates.L2EtcItem;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.commons.arrays.GArray;

public class RequestUnEquipItem extends L2GameClientPacket
{
	private int _slot;

	/**
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		_slot = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		// Нельзя снимать проклятое оружие
		if(_slot == L2Item.SLOT_LR_HAND && player.isCursedWeaponEquipped())
			return;

		if(_slot == L2Item.SLOT_L_HAND)
		{
			L2ItemInstance item = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

			if(item != null && (item.getItem().getItemType() == L2EtcItem.EtcItemType.ARROW || item.getItem().getItemType() == L2EtcItem.EtcItemType.BOLT))
				return;
		}

		if(player.isCombatFlagEquipped() && (_slot == L2Item.SLOT_LR_HAND || _slot == L2Item.SLOT_L_HAND || _slot == L2Item.SLOT_R_HAND))
			return;

		L2ItemInstance item = player.getInventory().getItemInBodySlot(_slot);

		if(item == null)
			return;

		L2ItemInstance weapon = player.getActiveWeaponInstance();
		GArray<L2ItemInstance> unequipped = player.getInventory().unEquipItemAndRecord(item);

		for(L2ItemInstance uneq : unequipped)
		{
			if(uneq == null)
				continue;

			player.sendDisarmMessage(uneq);

			if(weapon != null && uneq == weapon)
			{
				uneq.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				uneq.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
				player.abortAttack();
				player.abortCast();
			}
		}
		player.updateStats();
		player.sendPacket(new InventoryUpdate(unequipped));
	}
}