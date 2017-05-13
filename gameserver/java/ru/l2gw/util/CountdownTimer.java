package ru.l2gw.util;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;

import java.util.concurrent.ScheduledFuture;

/**
 * @author rage
 * @date 11.11.2010 14:01:37
 */
public abstract class CountdownTimer implements Runnable
{
	protected static final org.apache.commons.logging.Log _log = LogFactory.getLog(CountdownTimer.class); 
	private final GArray<Integer> _checkpoints;
	private long _sec;
	private long _scheduleSec;
	private int _currentPoint;
	private ScheduledFuture<?> _task;

	public CountdownTimer(String checkpoints, long sec)
	{
		_sec = sec;
		int start = 0;
		int point = 0;
		_checkpoints = new GArray<Integer>();
		for(String s : checkpoints.split(";"))
			if(s != null && !s.isEmpty())
			{
				int ss = Integer.parseInt(s);
				if(start == 0 && ss < _sec)
				{
					start = ss;
					_currentPoint = point;
				}
				_checkpoints.add(ss);
				point++;
			}

		_scheduleSec = _sec - start;
	}

	public void startTimer()
	{
		try
		{
			onStart();
			if(_currentPoint == 1 && _sec == _checkpoints.get(_currentPoint - 1))
				onCheckpoint(_sec);
		}
		catch(Throwable t)
		{
			_log.warn(getClass().getSimpleName() + " startTimer: " + t);
			t.printStackTrace();
		}

		_task = ThreadPoolManager.getInstance().scheduleGeneral(this, _scheduleSec * 1000L);
	}

	public void abortTimer(boolean cancel)
	{
		try
		{
			onTimerAborted();
		}
		catch(Throwable t)
		{
			_log.warn(getClass().getSimpleName() + " abortTimer: " + t);
			t.printStackTrace();
		}

		if(_task != null)
		{
			_task.cancel(cancel);
			_task = null;
		}
	}

	public void run()
	{
		if(_currentPoint + 1 < _checkpoints.size())
		{
			try
			{
				onCheckpoint(_checkpoints.get(_currentPoint));
			}
			catch(Throwable t)
			{
				_log.warn(getClass().getSimpleName() + " onCheckPoint: " + t);
				t.printStackTrace();
			}

			int s = _checkpoints.get(_currentPoint) - _checkpoints.get(_currentPoint + 1);
			_currentPoint++;

			_task = ThreadPoolManager.getInstance().scheduleGeneral(this, s * 1000L);
		}
		else
		{
			try
			{
				onFinish();
			}
			catch(Throwable t)
			{
				_log.warn(getClass().getSimpleName() + " onFinish: " + t);
				t.printStackTrace();
			}
		}
	}

	public void onStart() throws Throwable
	{
	}

	public abstract void onCheckpoint(long sec) throws Throwable;

	public abstract void onFinish() throws Throwable;

	public void onTimerAborted() throws Throwable
	{
	}
}
