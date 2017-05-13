package ru.l2gw.gameserver.taskmanager.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.scripts.Functions;
import ru.l2gw.gameserver.taskmanager.Task;
import ru.l2gw.gameserver.taskmanager.TaskManager;
import ru.l2gw.gameserver.taskmanager.TaskManager.ExecutedTask;
import ru.l2gw.gameserver.taskmanager.TaskTypes;


public class TaskEventRun extends Task
{
	private static final Log _log = LogFactory.getLog(TaskEventRun.class.getName());
	private String NAME;
	private String event_class;
	private String event_method;
	private String event_time;
	
	
	public TaskEventRun(String event_class, String event_method, String event_time)
	{
		this.event_class = event_class;
		this.event_method = event_method;
		this.event_time = event_time;	
		
		NAME = event_class+"."+event_method+"@"+event_time;
		
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", event_time+":00", "");
		TaskManager.getInstance().registerTask(this);
		TaskManager.getInstance().startAllTasks();
		
		
		//System.out.println("Event Task engine planned starts on "+event_time);
		
	}


	@Override
	public String getName()
	{
		return NAME;
	}


	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		Functions.callScripts (event_class, event_method, new Object[0], null);
	}


	@Override	
	public void initializate()
	{
		super.initializate();
	}

}
