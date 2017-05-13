package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.commons.arrays.ArrayUtils;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;

import java.util.Calendar;

/**
 * @author: rage
 * @date: 10.03.12 11:47
 */
public class TimeLimit implements IAdminLimit
{
	private final int startMin, startHour, endMin, endHour;
	private final GArray<Integer> days = new GArray<>(7);

	public TimeLimit(String start, String end, String daysList)
	{
		String[] s = start.split(":");
		String[] e = end.split(":");
		startHour = Integer.parseInt(s[0]);
		startMin = Integer.parseInt(s[1]);
		endHour = Integer.parseInt(e[0]);
		endMin = Integer.parseInt(e[1]);
		for(int day : ArrayUtils.toIntArray(daysList))
			days.add(day);
	}

	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		Calendar start = Calendar.getInstance();
		Calendar end = Calendar.getInstance();

		start.set(Calendar.HOUR_OF_DAY, startHour);
		start.set(Calendar.MINUTE, startMin);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.DAY_OF_YEAR, Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
		
		end.set(Calendar.HOUR_OF_DAY, endHour);
		end.set(Calendar.MINUTE, endMin);
		end.set(Calendar.SECOND, 0);
		end.set(Calendar.DAY_OF_YEAR, Calendar.getInstance().get(Calendar.DAY_OF_YEAR));

		return (days.isEmpty() || days.contains(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))) && start.getTimeInMillis() <= System.currentTimeMillis() && end.getTimeInMillis() >= System.currentTimeMillis();
	}
}
