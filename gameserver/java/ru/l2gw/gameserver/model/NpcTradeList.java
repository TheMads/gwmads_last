package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 30.08.11 18:32
 */
public class NpcTradeList
{
	private final int listId;
	private final int npcId;
	private final GArray<TradeItem> tradeItems;

	public NpcTradeList(int list, int npc)
	{
		listId = list;
		npcId = npc;
		tradeItems = new GArray<>();
	}

	public void addTradeItem(L2Item item, int markup, long count)
	{
		TradeItem ti = new TradeItem(item);
		ti.setCount(count);
		ti.setLimitCount(count);
		ti.setOwnersPrice((long) (item.getReferencePrice() * (markup / 100. + 1)));
		ti.setStorePrice(item.getReferencePrice());
		tradeItems.add(ti);
	}

	public int getNpcId()
	{
		return npcId;
	}

	public int getListId()
	{
		return listId;
	}

	public GArray<TradeItem> getTradeItems()
	{
		return tradeItems;
	}

	public boolean contains(int itemId)
	{
		for(TradeItem ti : tradeItems)
			if(ti.getItemId() == itemId)
				return true;

		return false;
	}

	public TradeItem getTradeItem(int itemId)
	{
		for(TradeItem ti : tradeItems)
			if(ti.getItemId() == itemId)
				return ti;

		return null;
	}

	@Override
	public String toString()
	{
		return "NpcTradeList[id=" + listId + ";npcId=" + npcId + "]";
	}
}
