package ru.l2gw.gameserver.model;

import java.util.Arrays;

public class L2QuestItem
{
	private short _itemId;
	private String[] _stateID = null;
	private String _questID = null;

	public L2QuestItem()
	{}

	public L2QuestItem(int id)
	{
		_itemId = (short) id;
	}

	public short getItemId()
	{
		return _itemId;
	}

	public void setItemId(short itemId)
	{
		_itemId = itemId;
	}

	public String[] getStateIDs()
	{
		return _stateID;
	}

	public String getQuestID()
	{
		return _questID;
	}

	@Override
	public String toString()
	{
		String out = "ItemID: " + getItemId() + " QuestID: " + getQuestID() + " StateID's: " + Arrays.toString(getStateIDs());
		return out;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof L2QuestItem)
		{
			L2QuestItem item = (L2QuestItem) o;
			return item.getItemId() == getItemId();
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return _itemId;
	}

}