package ru.l2gw.gameserver.model.base;

import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 10.12.2010 16:32:21
 */
public class MultiSellListContainer
{
	private int _listId;
	private boolean _showall = true;
	private boolean keep_enchanted = false;
	private boolean check_enchant_ingredient = false;
	private boolean is_dutyfree = false;
	public boolean nokey = false;
	public boolean community = false;

	public GArray<MultiSellEntry> entries = new GArray<MultiSellEntry>();

	public void setListId(int listId)
	{
		_listId = listId;
	}

	public int getListId()
	{
		return _listId;
	}

	public void setShowAll(boolean bool)
	{
		_showall = bool;
	}

	public boolean getShowAll()
	{
		return _showall;
	}

	public void setNoTax(boolean bool)
	{
		is_dutyfree = bool;
	}

	public boolean getNoTax()
	{
		return is_dutyfree;
	}

	public void setKeepEnchant(boolean bool)
	{
		keep_enchanted = bool;
	}

	public boolean getKeepEnchant()
	{
		return keep_enchanted;
	}

	public void setCheckEnchantIngredient(boolean b)
	{
		check_enchant_ingredient = b;
	}

	public boolean isCheckEnchantIngredient()
	{
		return check_enchant_ingredient;
	}

	public void addEntry(MultiSellEntry e)
	{
		entries.add(e);
	}

	public GArray<MultiSellEntry> getEntries()
	{
		return entries;
	}
}
