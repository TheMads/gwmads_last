package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.variation.VariationFee;
import ru.l2gw.gameserver.serverpackets.ExPutItemResultForVariationCancel;
import ru.l2gw.gameserver.tables.VariationData;

public class RequestConfirmCancelItem extends L2GameClientPacket
{
	// format: (ch)d
	int _itemId;

	@Override
	public void readImpl()
	{
		_itemId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		L2ItemInstance item = player.getInventory().getItemByObjectId(_itemId);

		if(item == null || player.getSessionVar("remove_aug") != null)
		{
			player.sendActionFailed();
			return;
		}

		if(!item.isAugmented())
		{
			player.sendPacket(Msg.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			return;
		}

		if(item.isPvP())
		{
			player.sendPacket(Msg.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		VariationFee fee = VariationData.getVariationFee(item.getItemId(), item.getAugmentation().getMineralId());
		if(fee == null)
		{
			player.sendPacket(Msg.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		player.sendPacket(new ExPutItemResultForVariationCancel(item, fee.cancel_fee));
	}
}