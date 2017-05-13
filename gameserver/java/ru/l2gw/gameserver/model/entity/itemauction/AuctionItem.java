package ru.l2gw.gameserver.model.entity.itemauction;

import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

/**
 * @author: rage
 * @date: 28.08.2010 12:37:18
 */
public class AuctionItem
{
	private final int _itemId;
	private final L2Item _itemTemplate;
	private final long _startBid;
	private final short _enchant;
	private final long _count;

	public AuctionItem(int itemId, short enchant, long startBid, long count)
	{
		_itemId = itemId;
		_itemTemplate = ItemTable.getInstance().getTemplate(itemId);
		_enchant = enchant;
		_startBid = startBid;
		_count = count;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public long getStartBid()
	{
		return _startBid;
	}
	
	public long getCount()
	{
		return _count;
	}

	public short getType2()
	{
		return (short) _itemTemplate.getType2();
	}

	public short getCustomType1()
	{
		return 0;
	}

	public int getBodyPart()
	{
		return _itemTemplate.getBodyPart();
	}

	public short getEnchantLevel()
	{
		return _enchant;
	}

	public short getCustomType2()
	{
		return 0;
	}
}
