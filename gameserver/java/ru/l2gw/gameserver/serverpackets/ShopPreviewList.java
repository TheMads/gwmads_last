package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.commons.arrays.GArray;

public class ShopPreviewList extends L2GameServerPacket
{
	private final int _listId;
	private final GArray<L2Item> _list;
	private final long _money;

	public ShopPreviewList(NpcTradeList list, long currentMoney, int expertiseIndex)
	{
		_listId = list.getListId();
		_list = new GArray<>();
		_money = currentMoney;

		for(TradeItem ti : list.getTradeItems())
		{
			L2Item item = ItemTable.getInstance().getTemplate(ti.getItemId());
			if(item.getCrystalType().externalOrdinal <= expertiseIndex)
				_list.add(item);
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xF5);
		writeD(0x13c0); //?
		writeQ(_money); // current money
		writeD(_listId);
		writeH(_list.size());
		for(L2Item item : _list)
		{
			writeD(item.getItemId());
			writeH(item.getType2()); // item type2

			if(item.getType1() != L2Item.TYPE1_ITEM_QUESTITEM_ADENA)
				writeH(item.getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
			else
				writeH(0x00); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand

			writeQ(item.getCrystalType().externalOrdinal == 0 ? 10 : item.getCrystalType().externalOrdinal * 50);
		}
	}
}