package ru.l2gw.gameserver.model;

/**
 * @author: rage
 * @date: 21.01.13 18:30
 */
public class PremiumItem
{
	private int id, itemId, ownerId;
	private long count;
	private String sender;

	public PremiumItem(int id, int itemId, int ownerId, long count, String sender)
	{
		this.id = id;
		this.itemId = itemId;
		this.ownerId = ownerId;
		this.count = count;
		this.sender = sender;
	}

	public int getId()
	{
		return id;
	}

	public int getItemId()
	{
		return itemId;
	}

	public int getOwnerId()
	{
		return ownerId;
	}

	public long getCount()
	{
		return count;
	}

	public String getSender()
	{
		return sender;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public void setCount(long count)
	{
		this.count = count;
	}
}
