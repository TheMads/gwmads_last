package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.maintenance.MaintenanceTask;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.Shutdown;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rage
 * @date 12.08.2009 11:44:31
 */
public class MaintenanceManager
{
	private static MaintenanceManager _instance;
	private Log _log = LogFactory.getLog("maintenance");

	private long _maintenanceTime;
	private Map<String, MaintenanceTask> _taskClasses = new HashMap<>();

	public static MaintenanceManager getInstance()
	{
		if(_instance == null)
			_instance = new MaintenanceManager();

		return _instance;
	}

	public void init()
	{
		Calendar calend = Calendar.getInstance();

		calend.set(Calendar.DAY_OF_WEEK, Config.MAINTENANCE_DAY);
		calend.set(Calendar.HOUR_OF_DAY, Config.MAINTENANCE_HOUR);
		calend.set(Calendar.MINUTE, 0);
		calend.set(Calendar.SECOND, 0);
		calend.set(Calendar.MILLISECOND, 0);

		if(System.currentTimeMillis() > calend.getTimeInMillis())
			calend.add(Calendar.HOUR, 24 * 7);

		_maintenanceTime = calend.getTimeInMillis();

		_log.info("MaintenanceManager: next maintenance time " + new Date(calend.getTimeInMillis()));

		if(_maintenanceTime - System.currentTimeMillis() < 3600000)
		{
			int sec = (int) ((_maintenanceTime - System.currentTimeMillis())) / 1000;
			Shutdown.getInstance().startTelnetShutdown("maintenance", sec, true);
		}
		else
		{
			_log.info("MaintenanceManager: schedule restart " + new Date(_maintenanceTime - 3600000));
			ThreadPoolManager.getInstance().scheduleGeneral(new RestartTask(), _maintenanceTime - System.currentTimeMillis() - 3600000);
		}

		maintenance();
	}

	private void maintenance()
	{
		Connection con;
		PreparedStatement stmt = null;
		PreparedStatement stmt2;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("SELECT * FROM maintenance_task WHERE status = 0 and datetime < ? ORDER BY id");
			stmt.setLong(1, System.currentTimeMillis() / 1000);

			rs = stmt.executeQuery();

			while(rs.next())
			{
				String taskName = rs.getString(2);
				String params = rs.getString(3);
				if(_taskClasses.containsKey(taskName))
				{
					boolean ret = _taskClasses.get(taskName).doTask(params);
					String result = _taskClasses.get(taskName).getLastResult();
					_log.info("MaintenanceManager: execute task: " + taskName + "(" + params + ") rs result: " + ret + " result string: " + result);

					stmt2 = con.prepareStatement("UPDATE maintenance_task SET status = 1, result = ?, lastResult = ? WHERE id = ?");
					stmt2.setBoolean(1, ret);
					stmt2.setString(2, result);
					stmt2.setInt(3, rs.getInt(1));
					stmt2.executeUpdate();
					stmt2.close();
				}
				else
				{
					MaintenanceTask task = createTask(taskName);
					if(task != null)
					{
						_taskClasses.put(taskName, task);

						boolean ret = task.doTask(params);
						_log.info("MaintenanceManager: execute task: " + taskName + "(" + params + ") rs result: " + ret + " result string: " + task.getLastResult());

						stmt2 = con.prepareStatement("UPDATE maintenance_task SET status = 1, result = ? WHERE id = ?");
						stmt2.setBoolean(1, ret);
						stmt2.setInt(2, rs.getInt(1));
						stmt2.executeUpdate();
						stmt2.close();
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("MaintenanceManager: error while executing query:");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(stmt, rs);
		}

	}

	private MaintenanceTask createTask(String taskName)
	{
		Constructor<?> constructor;
		try
		{
			constructor = Class.forName("ru.l2gw.extensions.maintenance." + taskName).getConstructors()[0];
		}
		catch(Exception e)
		{
			try
			{
				constructor = Scripts.getInstance().getClasses().get("maintenance." + taskName).getRawClass().getConstructors()[0];
			}
			catch(Exception e1)
			{
				_log.warn("MaintenanceManager: " + taskName + " class not found!");
				return null;
			}
		}

		MaintenanceTask task = null;

		try
		{
			task = (MaintenanceTask) constructor.newInstance();
		}
		catch(Exception e)
		{
			_log.warn("MaintenanceManager: can't create class " + taskName);
			e.printStackTrace();
		}

		return task;
	}

	public void addTask(String taskName, String params)
	{
		MaintenanceTask task = null;

		if(_taskClasses.containsKey(taskName))
			task = _taskClasses.get(taskName);
		else
			task = createTask(taskName);

		if(task != null)
			task.addTask(params);
		else
			_log.warn("MaintenanceManager: task " + taskName + "(" + params + ") not found!");
	}

	public int getMaintenanceTime()
	{
		return (int)(_maintenanceTime / 1000);
	}

	private class RestartTask implements Runnable
	{
		public void run()
		{
			Shutdown.getInstance().startTelnetShutdown("maintenance", 3600, true);
		}
	}
}
