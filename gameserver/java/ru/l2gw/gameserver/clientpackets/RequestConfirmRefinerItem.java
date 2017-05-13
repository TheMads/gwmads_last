package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.variation.VariationFee;
import ru.l2gw.gameserver.serverpackets.ExPutIntensiveResultForVariationMake;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.VariationData;

public class RequestConfirmRefinerItem extends AbstractRefinePacket
{
	// format: (ch)dd
	private int _targetItemObjId;
	private int _refinerItemObjId;

	@Override
	public void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		L2ItemInstance targetItem = player.getInventory().getItemByObjectId(_targetItemObjId);
		L2ItemInstance refinerItem = player.getInventory().getItemByObjectId(_refinerItemObjId);

		if(targetItem == null || refinerItem == null || !isValid(player, targetItem, refinerItem, true))
			return;

		final VariationFee fee = VariationData.getVariationFee(targetItem.getItemId(), refinerItem.getItemId());

		if(fee == null)
		{
			player.sendPacket(Msg.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		final int refinerItemId = refinerItem.getItemId();

		player.sendPacket(new SystemMessage(SystemMessage.REQUIRES_S1_S2).addNumber(fee.fee_count).addItemName(fee.fee_item_id));
		player.sendPacket(new ExPutIntensiveResultForVariationMake(_refinerItemObjId, refinerItemId, fee.fee_item_id, fee.fee_count));
	}
}