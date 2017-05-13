package ru.l2gw.gameserver.model.playerSubOrders;

import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 23.06.2010 10:36:24
 */
public class TeleportBookmark
{
	private int _slot;
	private String _name, _acronym;
	private int _icon;
	private Location _loc;

	public TeleportBookmark(int slot, int icon, String name, String acronym, Location loc)
	{
		_slot = slot;
		_icon = icon;
		_name = name;
		_acronym = acronym;
		_loc = loc;
	}

	public int getSlot()
	{
		return _slot;
	}

	public int getIcon()
	{
		return _icon;
	}

	public String getName()
	{
		return _name;
	}

	public String getAcronym()
	{
		return _acronym;
	}

	public Location getLoc()
	{
		return _loc;
	}

	public void setName(String name)
	{
		_name = name;
	}

	public void setAcronym(String acronym)
	{
		_acronym = acronym;
	}

	public void setIcon(int icon)
	{
		_icon = icon;
	}
}
