package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2TradeList;
import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.commons.arrays.GArray;

public class PrivateStoreManageListBuy extends AbstractItemPacket
{
	private GArray<TradeItem> buylist = new GArray<TradeItem>();
	private int buyer_id;
	long buyer_adena;
	private L2TradeList _list;

	/**
	 * Окно управления личным магазином продажи
	 * @param buyer
	 */
	public PrivateStoreManageListBuy(L2Player buyer)
	{
		buyer_id = buyer.getObjectId();
		buyer_adena = buyer.getAdena();

		for(TradeItem e : buyer.getBuyList())
		{
			L2Item tempItem = ItemTable.getInstance().getTemplate(e.getItemId());
			if(tempItem == null)
				continue;

			buylist.add(e);
		}

		_list = new L2TradeList(0);
		for(L2ItemInstance item : buyer.getInventory().getItems())
			if(item.getItemId() == 5575 || item.getItem().getType2() != L2Item.TYPE2_QUEST && item.getItem().getType2() != L2Item.TYPE2_MONEY && item.canBeTraded(buyer))
			{
				//for(TradeItem ti : buyer.getSellList())
				//	if(ti.getItemId() == item.getItemId())
				//		continue;
				_list.addItem(item);
			}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xBD);
		//section 1
		writeD(buyer_id);
		writeQ(buyer_adena);

		//section2
		writeD(_list.getItems().size());//for potential sells
		for(L2ItemInstance temp : _list.getItems())
		{
			writeItemInfo(temp);
			writeQ(temp.getReferencePrice() * 2);
		}

		//section 3
		writeD(buylist.size());//count for any items already added for sell
		for(TradeItem temp : buylist)
		{
			writeItemInfo(temp);
			writeQ(temp.getOwnersPrice());
			writeQ(temp.getStorePrice() * 2);
			writeQ(temp.getCount());
		}
	}
}