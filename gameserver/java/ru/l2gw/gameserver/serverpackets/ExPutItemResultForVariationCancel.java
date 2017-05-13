package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public class ExPutItemResultForVariationCancel extends L2GameServerPacket
{
	private int _itemObjId, _itemId, _aug1, _aug2;
	private long _price;

	public ExPutItemResultForVariationCancel(L2ItemInstance item, long price)
	{
		_itemObjId = item.getObjectId();
		_itemId = item.getItemId();
		_price = price;
		_aug1 = (short) item.getAugmentation().getAugmentationId();
		_aug2 = item.getAugmentation().getAugmentationId() >> 16;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x58);
		writeD(_itemObjId);
		writeD(_itemId);
		writeD(_aug1);
		writeD(_aug2);
		writeQ(_price);
		writeD(0x01);
	}
}