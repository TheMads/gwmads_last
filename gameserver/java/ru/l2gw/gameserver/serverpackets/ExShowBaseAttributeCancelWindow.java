package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.templates.L2Item;

/**
 * @author rage
 */
public class ExShowBaseAttributeCancelWindow extends L2GameServerPacket
{
	private final GArray<L2ItemInstance> _items = new GArray<L2ItemInstance>();

	public ExShowBaseAttributeCancelWindow(L2Player player)
	{
		for(L2ItemInstance i : player.getInventory().getItemsList())
		{
			if(i.getAttributeElementValue(L2Item.ATTRIBUTE_FIRE) == 0 && i.getAttributeElementValue(L2Item.ATTRIBUTE_WATER) == 0 && 
			i.getAttributeElementValue(L2Item.ATTRIBUTE_EARTH) == 0 && i.getAttributeElementValue(L2Item.ATTRIBUTE_WIND) == 0 && 
			i.getAttributeElementValue(L2Item.ATTRIBUTE_DARK) == 0 && i.getAttributeElementValue(L2Item.ATTRIBUTE_HOLY) == 0 || getAttributeRemovePrice(i) == 0)
				continue;
			_items.add(i);
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x75);
		writeD(_items.size());
		for(L2ItemInstance i : _items)
		{
			writeD(i.getObjectId());
			writeQ(getAttributeRemovePrice(i));
		}
	}

	public static long getAttributeRemovePrice(L2ItemInstance item)
	{
		switch(item.getCrystalType())
		{
			case S:
				return item.getItem().getType2() == L2Item.TYPE2_WEAPON ? 50000 : 40000;
			case S80:
				return item.getItem().getType2() == L2Item.TYPE2_WEAPON ? 100000 : 80000;
			case S84:
				return item.getItem().getType2() == L2Item.TYPE2_WEAPON ? 200000 : 160000;
		}
		return 0;
	}
}