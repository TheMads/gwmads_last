package ru.l2gw.gameserver.model.entity.siege;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;

import java.util.Calendar;

public class SiegeEndTask implements Runnable
{
	private Siege _siege;
	private static Log _log = LogFactory.getLog("siege");
	public SiegeEndTask(Siege siege)
	{
		_siege = siege;
	}

	public void run()
	{
		if(!_siege.isInProgress())
			return;

		try
		{
			long timeRemaining = _siege.getSiegeEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
			_log.info("Siege End: " + timeRemaining + " for " + _siege.getSiegeUnit());

			if(timeRemaining > 0)
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining); // Prepare task for second count down
			else
				_siege.endSiege();
		}
		catch(Throwable t)
		{}
	}
}
