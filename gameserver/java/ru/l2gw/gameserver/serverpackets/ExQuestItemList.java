package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 16.12.10 17:12
 */
public class ExQuestItemList extends AbstractItemPacket
{
	private GArray<L2ItemInstance> _items;
	//private PcInventory _inventory;

	public ExQuestItemList(GArray<L2ItemInstance> items)
	{
		_items = items;
		//_inventory = inv;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xC7);
		writeH(_items.size());
		for(L2ItemInstance item : _items)
			writeItemInfo(item);
		/*
		if (_inventory.hasInventoryBlock())
		{
			writeH(_inventory.getBlockItems().length);
			writeC(_inventory.getBlockMode());
			for(int i : _inventory.getBlockItems())
				writeD(i);
		}
		else
		*/
			writeH(0x00);
	}
}
