package ru.l2gw.gameserver.model.entity.olympiad;

import ru.l2gw.gameserver.templates.StatsSet;

import java.util.Comparator;

/**
 * @author admin
 * @date 13.05.11 11:35
 */
public class RateComparator implements Comparator<StatsSet>
{
	private static RateComparator _instance;

	public static RateComparator getInstance()
	{
		if(_instance == null)
			_instance = new RateComparator();
		return _instance;
	}

	public int compare(StatsSet o1, StatsSet o2)
	{
		if(o1 != null && o2 != null)
			return o2.getInteger("prev_points", 0) - o1.getInteger("prev_points", 0);

		return 0;
	}
}
