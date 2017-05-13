/**
 *
 */
package ru.l2gw.gameserver.taskmanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.taskmanager.TaskManager.ExecutedTask;

import java.util.concurrent.ScheduledFuture;

/**
 * @author Layane
 *
 */
public abstract class Task
{
	private static Log _log = LogFactory.getLog(Task.class.getName());

	public void initializate()
	{
		if(Config.DEBUG)
			_log.info("Task" + getName() + " inializate");
	}

	public ScheduledFuture<?> launchSpecial(@SuppressWarnings("unused") ExecutedTask instance)
	{
		return null;
	}

	public abstract String getName();

	public abstract void onTimeElapsed(ExecutedTask task);

	public void onDestroy()
	{}
}
