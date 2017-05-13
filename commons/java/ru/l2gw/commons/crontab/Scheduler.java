package ru.l2gw.commons.crontab;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: rage
 * @date: 03.03.12 12:11
 */
public class Scheduler
{
	private Calendar calendar;
	private static final int DAYS_IN_WEEK = 7;

	private final Map<String, Map<Integer, String>> schedule = new HashMap<>();
	private static Scheduler _instance;
	private boolean debugging = true;

	private boolean testing = false;

	private static final Log _log = LogFactory.getLog(Scheduler.class.getName());

	public static Scheduler getInstance()
	{
		if(_instance == null)
		{
			_instance = new Scheduler();
		}
		return _instance;
	}

	private Scheduler()
	{
		log("WildMagic engine message: Event scheduler v1.3.1 initialized");
	}

	public void setDebug(Boolean debugging)
	{
		this.debugging = debugging;
	}

	public void setTesting(Boolean testing)
	{
		this.testing = testing;
	}

	public void setTestCalendar(Calendar calendar)
	{
		if(!testing)
		{
			log("Couldn't sets up testing calendar. Scheduler not in testing mode!");
			return;
		}

		this.calendar = calendar;
		log("New test calendar has been set to " + calendar.getTime());
	}

	private void log(String text)
	{
		if(debugging)
			_log.info(text);
	}

	private Calendar currentCalendar()
	{
		if(testing)
		{
			return calendar;
		}
		return new GregorianCalendar();
	}

	private boolean validateSchedule(Map<Integer, String> schedule)
	{
		Pattern pattern = Pattern.compile("[0-9]{2}:[0-9]{2}(,[0-9]{2}:[0-9]{2}){0,1440}");

		if(schedule == null || schedule.isEmpty())
		{
			_log.info("Schedule shouldn't be null or empty");
			return false;
		}

		for(Integer key : schedule.keySet())
		{
			if((key < 1) || (key > 7))
			{
				_log.info("Schedule may contain only day of week constants");
				return false;
			}

			String plan = schedule.get(key);

			if(plan == null)
			{
				_log.info("Plan shouldn't be null. Please check your schedule");
				return false;
			}

			Matcher regexp = pattern.matcher(plan);

			if(!regexp.matches())
			{
				_log.info("Plan [" + plan +
						"] has invalid format. You should enumirate times with format HH:MM and separate it by comma");
				return false;
			}

		}

		return true;
	}

	public void register(String taskName, Map<Integer, String> schedule)
	{
		if(this.schedule.containsKey(taskName))
		{
			log("Couldn't register single task twice");
			return;
		}

		if(!validateSchedule(schedule))
		{
			log("Specified schedule not valid!");
			return;
		}

		this.schedule.put(taskName, schedule);
		log("Registered new schedule for task \"" + taskName + "\"");
	}

	public void unregister(String taskName)
	{
		if(schedule.remove(taskName) != null)
			log("Task \"" + taskName + "\" successfuly unregistered");
	}

	private long getNextExecTime(Map<Integer, String> rawSchedule)
	{
		int today = currentCalendar().get(7);
		long result = -1L;

		for(int checkday = 1; checkday <= DAYS_IN_WEEK; checkday++)
		{
			String rawPlan = rawSchedule.get(checkday);

			if(rawPlan == null)
			{
				continue;
			}
			String[] plan = rawPlan.split(",");

			int days =
					checkday < today ? DAYS_IN_WEEK - (today - checkday) : checkday - today;

			for(String execPoint : plan)
			{
				Calendar c = new GregorianCalendar();
				c.setTime(currentCalendar().getTime());
				c.add(5, days);
				c.set(9, 0);

				String[] timePair = execPoint.split(":");
				int hour = Integer.parseInt(timePair[0]);
				int minute = Integer.parseInt(timePair[1]);
				c.set(10, hour);
				c.set(12, minute);
				c.set(13, 0);
				c.set(14, 0);

				if((days == 0) && (c.getTimeInMillis() <= currentCalendar().getTimeInMillis()))
				{
					c.add(5, DAYS_IN_WEEK);
				}

				long val = c.getTimeInMillis();

				if((result == -1L) || (val <= result))
				{
					result = val;
				}
			}

		}

		return result;
	}

	public long getNextInterval(String taskName)
	{
		long nextExecTime = getNextExecTime(schedule.get(taskName));
		long result = nextExecTime - currentCalendar().getTimeInMillis();

		log("*** Next execution for task \"" + taskName + "\" will be at " + new Date(nextExecTime));

		return result;
	}
}
