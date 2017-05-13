/**
 *
 */
package ru.l2gw.gameserver.taskmanager.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.taskmanager.Task;
import ru.l2gw.gameserver.taskmanager.TaskManager;
import ru.l2gw.gameserver.taskmanager.TaskManager.ExecutedTask;
import ru.l2gw.gameserver.taskmanager.TaskTypes;

/**
 * @author Layane
 *
 */
public class TaskRecom extends Task
{
	private static final Log _log = LogFactory.getLog(TaskRecom.class.getName());
	private static final String NAME = "sp_recommendations";

	/* (non-Javadoc)
	 * @see ru.l2gw.gameserver.taskmanager.Task#getName()
	 */
	@Override
	public String getName()
	{
		return NAME;
	}


	/* (non-Javadoc)
	 * @see ru.l2gw.gameserver.taskmanager.Task#onTimeElapsed(ru.l2gw.gameserver.taskmanager.TaskManager.ExecutedTask)
	 */
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		_log.debug("Recommendation Global Task: launched.");
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
		{
			player.getRecSystem().restart();
			player.sendUserInfo(false);
		}
		_log.debug("Recommendation Global Task: completed.");
	}

	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "06:30:00", "");
	}

}
