package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.variation.VariationFee;
import ru.l2gw.gameserver.serverpackets.ExPutCommissionResultForVariationMake;
import ru.l2gw.gameserver.tables.VariationData;

public class RequestConfirmGemStone extends AbstractRefinePacket
{
	// format: (ch)dddd
	private int _targetItemObjId;
	private int _refinerItemObjId;
	private int _gemstoneItemObjId;
	private long _gemstoneCount;

	@Override
	public void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
		_gemstoneItemObjId = readD();
		_gemstoneCount = readQ();
	}

	@Override
	public void runImpl()
	{
		if(_gemstoneCount <= 0)
			return;

		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		L2ItemInstance targetItem = player.getInventory().getItemByObjectId(_targetItemObjId);
		L2ItemInstance refinerItem = player.getInventory().getItemByObjectId(_refinerItemObjId);
		L2ItemInstance gemstoneItem = player.getInventory().getItemByObjectId(_gemstoneItemObjId);

		if(targetItem == null || refinerItem == null || gemstoneItem == null || !isValid(player, targetItem, refinerItem, gemstoneItem, true))
			return;

		final VariationFee fee = VariationData.getVariationFee(targetItem.getItemId(), refinerItem.getItemId());

		if(fee == null || fee.fee_item_id != gemstoneItem.getItemId())
		{
			player.sendPacket(Msg.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		if(gemstoneItem.getCount() < fee.fee_count)
		{
			player.sendPacket(Msg.GEMSTONE_QUANTITY_IS_INCORRECT);
			return;
		}

		player.sendPacket(new ExPutCommissionResultForVariationMake(_gemstoneItemObjId, gemstoneItem.getItemId(), _gemstoneCount));
		player.sendPacket(Msg.PRESS_THE_AUGMENT_BUTTON_TO_BEGIN);
	}
}