package ru.l2gw.gameserver.model.entity.itemauction;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

/**
 * @author: rage
 * @date: 28.08.2010 12:50:24
 */
public class ItemAuctionTemplate
{
	private final GArray<AuctionItem> _items;
	private final int _brokerNpcId;
	private final int _startDay;
	private final int _startHour;
	private final int _startMin;
	private final long _auctionTime;

	public ItemAuctionTemplate(int npcId, int startDay, int startHour, int startMin, long time)
	{
		_brokerNpcId = npcId;
		_startDay = startDay;
		_startHour = startHour;
		_startMin = startMin;
		_auctionTime = time;
		_items = new GArray<AuctionItem>();
	}

	public void addAuctionItem(AuctionItem item)
	{
		_items.add(item);
	}

	public AuctionItem getAuctionItemById(int itemId)
	{
		for(AuctionItem item : _items)
			if(item.getItemId() == itemId)
				return item;

		return _items.get(0);
	}

	public int getBrokerId()
	{
		return _brokerNpcId;
	}

	public long getAuctionTime()
	{
		return _auctionTime;
	}

	public int getStartDay()
	{
		return _startDay;
	}

	public int getStartHour()
	{
		return _startHour;
	}

	public int getStartMin()
	{
		return _startMin;
	}

	public AuctionItem getRandomItem()
	{
		return _items.get(Rnd.get(_items.size()));
	}
}
