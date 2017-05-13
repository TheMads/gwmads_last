package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2TradeList;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 * Format: c ddh[hdddhhd]
 * c - id (0xE8)
 *
 * d - money
 * d - manor id
 * h - size
 * [
 * h - item type 1
 * d - object id
 * d - item id
 * d - count
 * h - item type 2
 * h
 * d - price
 * ]
 *
 */
public final class BuyListSeed extends AbstractItemPacket
{
	private int _manorId;
	private GArray<L2ItemInstance> _list = new GArray<L2ItemInstance>();
	private long _money;

	public BuyListSeed(L2TradeList list, int manorId, long currentMoney)
	{
		_money = currentMoney;
		_manorId = manorId;
		_list = list.getItems();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xe9);

		writeQ(_money); // current money
		writeD(0x00); // God UNK
		writeD(_manorId); // manor id

		writeH(_list.size()); // list length

		for(L2ItemInstance item : _list)
		{
			writeItemInfo(item);
			//writeD(item.getObjectId());
			//writeD(item.getItemId());
			//writeD(item.getEquipSlot());
			//writeQ(item.getCount());
			//writeH(item.getType2());
			//writeH(item.getCustomType1());
			//writeH(item.isEquipped() ? 1 : 0);
			//writeD(item.getBodyPart());
			//writeH(item.getEnchantLevel());
			//writeH(item.getCustomType2());
			//writeH(item.getAugmentationId());
			//writeH(0x00);
			//writeD(item.getShadowLifeTime());
			//writeD(item.getTemporalLifeTime());
			//writeH(0x01); // блокировать ли вещь( 01 нет, 00 да)
			//writeH(item.getAttackElement()[0]);
			//writeH(item.getAttackElement()[1]);
			//writeH(item.getDefenceFire());
			//writeH(item.getDefenceWater());
			//writeH(item.getDefenceWind());
			//writeH(item.getDefenceEarth());
			//writeH(item.getDefenceHoly());
			//writeH(item.getDefenceUnholy());
			//writeH(item.getEnchantOption()[0]);
			//writeH(item.getEnchantOption()[1]);
			//writeH(item.getEnchantOption()[2]);
			//writeD(item.getVisualId());
		}
	}
}