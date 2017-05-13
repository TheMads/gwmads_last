package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.variation.VariationFee;
import ru.l2gw.gameserver.serverpackets.ExVariationResult;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.VariationData;

public final class RequestRefine extends AbstractRefinePacket
{
	// format: (ch)dddd
	private int targetItemObjId;
	private int refinerItemObjId;
	private int gemstoneItemObjId;
	private long gemstoneCount;

	@Override
	protected void readImpl()
	{
		targetItemObjId = readD();
		refinerItemObjId = readD();
		gemstoneItemObjId = readD();
		gemstoneCount = readQ();
	}

	@Override
	protected void runImpl()
	{
		if(gemstoneCount < 0)
			return;

		L2Player player = getClient().getPlayer();
		L2ItemInstance targetItem = player.getInventory().getItemByObjectId(targetItemObjId);
		L2ItemInstance refinerItem = player.getInventory().getItemByObjectId(refinerItemObjId);
		L2ItemInstance gemstoneItem = player.getInventory().getItemByObjectId(gemstoneItemObjId);
		VariationFee fee = null;

		if(targetItem == null || refinerItem == null || gemstoneItem == null || !isValid(player, targetItem, refinerItem, gemstoneItem, false) || (fee = VariationData.getVariationFee(targetItem.getItemId(), refinerItem.getItemId())) == null || fee.fee_item_id != gemstoneItem.getItemId() || fee.fee_count > gemstoneItem.getCount())
		{
			player.sendPacket(new ExVariationResult(0, 0, 0));
			player.sendPacket(Msg.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}

		if(!player.destroyItem("Refine", refinerItem.getObjectId(), 1, null, true) || !player.destroyItem("Refine", gemstoneItemObjId, fee.fee_count, null, true))
		{
			player.sendPacket(new ExVariationResult(0,0,0));
			player.sendPacket(Msg.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}

		// unequip item
		if(targetItem.isEquipped())
			player.getInventory().unEquipItemAndSendChanges(targetItem);

		targetItem.setAugmentation(VariationData.generateRandomVariation(targetItem, refinerItem.getItemId()));

		// send an inventory update packet
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		player.sendPacket(iu);
		player.sendUserInfo(false);

		int stat12 = 0x0000FFFF & targetItem.getAugmentation().getAugmentationId();
		int stat34 = targetItem.getAugmentation().getAugmentationId() >> 16;
		player.sendPacket(new ExVariationResult(stat12, stat34, 1));
		player.sendPacket(new SystemMessage(SystemMessage.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED));
		player.sendChanges();
	}
}