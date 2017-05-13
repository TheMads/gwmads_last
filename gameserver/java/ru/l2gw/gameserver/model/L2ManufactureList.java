package ru.l2gw.gameserver.model;

import java.util.ArrayList;

public class L2ManufactureList
{
	private ArrayList<L2ManufactureItem> _list;
	private boolean _confirmed;
	private String _manufactureStoreName;

	public L2ManufactureList()
	{
		_list = new ArrayList<L2ManufactureItem>();
		_confirmed = false;
	}

	public int size()
	{
		return _list.size();
	}

	public void setConfirmedTrade(boolean x)
	{
		_confirmed = x;
	}

	public boolean hasConfirmed()
	{
		return _confirmed;
	}

	/**
	 * @param manufactureStoreName The _manufactureStoreName to set.
	 */
	public void setStoreName(String manufactureStoreName)
	{
		_manufactureStoreName = manufactureStoreName;
	}

	/**
	 * @return Returns the _manufactureStoreName.
	 */
	public String getStoreName()
	{
		return _manufactureStoreName;
	}

	public void add(L2ManufactureItem item)
	{
		_list.add(item);
	}

	public ArrayList<L2ManufactureItem> getList()
	{
		return _list;
	}

	public void setList(ArrayList<L2ManufactureItem> list)
	{
		_list = list;
	}
}