package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.variation.VariationFee;
import ru.l2gw.gameserver.serverpackets.ExVariationCancelResult;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.VariationData;

public final class RequestRefineCancel extends L2GameClientPacket
{
	//format: (ch)d
	private int _targetItemObjId;

	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		L2ItemInstance targetItem = player.getInventory().getItemByObjectId(_targetItemObjId);
		player.setSessionVar("remove_aug", "true");

		// cannot remove augmentation from a not augmented item
		if(!targetItem.isAugmented())
		{
			player.sendPacket(new SystemMessage(SystemMessage.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM));
			return;
		}

		VariationFee fee = VariationData.getVariationFee(targetItem.getItemId(), targetItem.getAugmentation().getMineralId());

		if(fee == null)
		{
			player.sendPacket(Msg.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		// try to reduce the players adena
		if(!player.reduceAdena("Refine", fee.cancel_fee, player.getLastNpc(), true))
		{
			player.sendPacket(new ExVariationCancelResult(false));
			return;
		}

		if(targetItem.isEquipped())
			player.getInventory().unEquipItemAndSendChanges(targetItem);

		// cancel boni
		targetItem.getAugmentation().removeBonus(player);

		// remove the augmentation
		targetItem.removeAugmentation();

		// send ExVariationCancelResult
		player.sendPacket(new ExVariationCancelResult(true));

		// send inventory update
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		player.sendPacket(iu);

		// send system message
		SystemMessage sm = new SystemMessage(SystemMessage.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1);
		sm.addItemName(targetItem.getItemId());
		player.sendPacket(sm);

		player.broadcastUserInfo(true);
		player.setSessionVar("remove_aug", null);
	}
}