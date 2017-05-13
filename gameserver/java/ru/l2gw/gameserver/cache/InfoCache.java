package ru.l2gw.gameserver.cache;

import java.util.HashMap;

public abstract class InfoCache
{
	private static final HashMap<Integer, String> _droplistCache = new HashMap<>();

	public static void addToDroplistCache(final int id, final String list)
	{
		_droplistCache.put(id, list);
	}

	public static String getFromDroplistCache(final int id)
	{
		return _droplistCache.get(id);
	}
}
