package ru.l2gw.gameserver.model.entity.olympiad;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.templates.StatsSet;

import java.util.Comparator;

/**
 * @author rage
 * @date 04.05.11 14:43
 */
public class NoblesComparator implements Comparator<Integer>
{
	private static NoblesComparator _instance;

	public static NoblesComparator getInstance()
	{
		if(_instance == null)
			_instance = new NoblesComparator();
		return _instance;
	}

	public int compare(Integer i1, Integer i2)
	{
		StatsSet o1 = Olympiad._nobles.get(i1);
		StatsSet o2 = Olympiad._nobles.get(i2);

		if(o1 != null && o2 != null)
		{
			if(Math.abs(o1.getInteger("points", 0) - o2.getInteger("points", 0)) < Config.ALT_OLY_POINTS_DIFF)
				return 0;

			return o1.getInteger("points", 0) - o2.getInteger("points", 0);
		}

		return 0;
	}
}
