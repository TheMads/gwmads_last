package ru.l2gw.gameserver.model.entity.fieldcycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.fieldcycle.FieldTasks.DropTimeTask;
import ru.l2gw.gameserver.model.entity.fieldcycle.FieldTasks.ExpireTask;
import ru.l2gw.util.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

/**
 * @author: rage
 * @date: 11.12.11 16:50
 */
public class FieldCycle
{
	private static final Log _log = LogFactory.getLog("fieldcycle");
	private final int id;
	private int currentStep;
	private long currentPoint;
	private long lastPointChangeTime;
	private long lastStepChangeTime;
	private long lastStoreTime;
	private FieldStep[] fieldSteps;
	private IFieldCycleMaker[] onStepChanged;
	private IFieldCycleMaker[] onStepExpired;
	private FieldStep nextStep = null;
	private ScheduledFuture<?> dropTask = null;
	private ScheduledFuture<?> expireTask = null;

	public FieldCycle(int id)
	{
		this.id = id;
	}

	public int getStep()
	{
		return currentStep;
	}

	public long getPoint()
	{
		return currentPoint;
	}

	public int getMapString()
	{
		if(fieldSteps[currentStep] != null)
			return fieldSteps[currentStep].getMapString();

		return 0;
	}

	public Location getMapLoc()
	{
		if(fieldSteps[currentStep] != null)
			return fieldSteps[currentStep].getMapLoc();

		return null;
	}

	public synchronized void addPoint(String process, long point, L2Character actor)
	{
		if(currentPoint + point < 0)
			currentPoint = 0;
		else
			currentPoint += point;

		lastPointChangeTime = System.currentTimeMillis();

		_log.info(process + ": fc: " + id + " add: " + point + (actor == null ? "" : " " + actor));

		if(nextStep != null && currentPoint >= nextStep.getPoint() && (fieldSteps[currentStep] == null || lastStepChangeTime + fieldSteps[currentStep].getLockTime() < System.currentTimeMillis()))
			setStep(process, nextStep.getStep(), actor);
		else if(lastStoreTime + 600000 < System.currentTimeMillis())
			store();
	}

	public synchronized void setStep(String process, int step, L2Character actor)
	{
		if(currentStep == step || step < 0 || step >= fieldSteps.length)
			return;

		lastPointChangeTime = lastStepChangeTime = System.currentTimeMillis();

		if(fieldSteps[currentStep] != null)
		{
			fieldSteps[currentStep].stop();
			if(dropTask != null)
			{
				dropTask.cancel(false);
				dropTask = null;
			}
			if(expireTask != null)
			{
				expireTask.cancel(false);
				expireTask = null;
			}
		}

		int oldStep = currentStep;
		currentStep = step;
		if(fieldSteps[currentStep] != null)
		{
			currentPoint = fieldSteps[currentStep].getPoint();
			fieldSteps[currentStep].start();
			if(fieldSteps[currentStep].getDropTime() > 0)
			{
				startDropTask();
			}
			if(fieldSteps[currentStep].getExpireTime() > 0)
			{
				_log.info("fc: " + id + " expire time: " + new Date(fieldSteps[currentStep].getExpireTime()) + " sec: " + (fieldSteps[currentStep].getExpireTime() - System.currentTimeMillis()) / 1000);
				expireTask = ThreadPoolManager.getInstance().scheduleGeneral(new ExpireTask(this, fieldSteps[currentStep]), fieldSteps[currentStep].getExpireTime() - System.currentTimeMillis());
			}
		}
		else
		{
			currentPoint = 0;
		}

		setNextStep();
		store();

		_log.info(process + ": fc: " + id + " set step: "+ oldStep + " -> " + step + (fieldSteps[currentStep].getLockTime() > 0 ? " locked time: " + new Date(lastPointChangeTime + fieldSteps[currentStep].getLockTime()) : "") + (actor == null ? "" : " " + actor));

		if(onStepChanged != null)
		{
			for(IFieldCycleMaker maker : onStepChanged)
				maker.onFieldCycleChanged(id, oldStep, currentStep);
		}
	}

	public long getLastPointChangeTime()
	{
		return lastPointChangeTime;
	}

	public void addStep(FieldStep step)
	{
		if(fieldSteps == null)
		{
			fieldSteps = new FieldStep[step.getStep() + 1];
			fieldSteps[step.getStep()] = step;
		}
		else if(fieldSteps.length <= step.getStep())
		{
			int len = fieldSteps.length;
			FieldStep[] tmp = new FieldStep[step.getStep() + 1];
			System.arraycopy(fieldSteps, 0, tmp, 0, len);
			tmp[step.getStep()] = step;
			fieldSteps = tmp;
		}
		else
			fieldSteps[step.getStep()] = step;
	}

	public void registerStepChanged(IFieldCycleMaker maker)
	{
		if(onStepChanged == null)
			onStepChanged = new IFieldCycleMaker[]{ maker };
		else
		{
			int len = onStepChanged.length;
			IFieldCycleMaker[] tmp = new IFieldCycleMaker[len + 1];
			System.arraycopy(onStepChanged, 0, tmp, 0, len);
			tmp[len] = maker;
			onStepChanged = tmp;
		}
	}

	public void registerStepExpired(IFieldCycleMaker maker)
	{
		if(onStepExpired == null)
			onStepExpired = new IFieldCycleMaker[]{ maker };
		else
		{
			int len = onStepExpired.length;
			IFieldCycleMaker[] tmp = new IFieldCycleMaker[len + 1];
			System.arraycopy(onStepExpired, 0, tmp, 0, len);
			tmp[len] = maker;
			onStepExpired = tmp;
		}
	}

	public void setCurrentPoint(long point)
	{
		currentPoint = point;
	}

	public void setCurrentStep(int step)
	{
		currentStep = step;
	}

	public void setStepChangeTime(long time)
	{
		lastStepChangeTime = time;
	}

	public void setPointChangeTime(long time)
	{
		lastPointChangeTime = time;
	}

	public void setNextStep()
	{
		nextStep = currentStep + 1 < fieldSteps.length ? fieldSteps[currentStep + 1] : null;
	}

	public void onExpireTime()
	{
		_log.info("fc: " + id + " expire time.");
		if(onStepExpired != null)
		{
			for(IFieldCycleMaker maker : onStepExpired)
			{
				maker.onFieldCycleExpired(id, currentStep, 0);
				_log.info("fc: " + id + " notify expire: " + maker);
			}
		}
	}

	public void startCycle()
	{
		setNextStep();

		if(fieldSteps[currentStep] == null)
			return;

		FieldStep step = fieldSteps[currentStep];
		step.start();

		if(step.getDropTime() > 0)
		{
			if(lastPointChangeTime + step.getDropTime() < System.currentTimeMillis())
				setStep("DROP_TIME", Math.max(currentStep - 1, 0), null);
			else
				startDropTask();
		}
		if(step.getExpireTime() > 0)
		{
			_log.info("fc: " + id + " expire time: " + new Date(step.getExpireTime()) + " sec: " + (step.getExpireTime() - System.currentTimeMillis()) / 1000);
			expireTask = ThreadPoolManager.getInstance().scheduleGeneral(new ExpireTask(this, step), step.getExpireTime() - System.currentTimeMillis());
		}
	}

	public void startDropTask()
	{
		if(dropTask != null)
			dropTask.cancel(false);

		long delay = lastPointChangeTime + fieldSteps[currentStep].getDropTime() - System.currentTimeMillis() + 20;
		dropTask = ThreadPoolManager.getInstance().scheduleGeneral(new DropTimeTask(this, fieldSteps[currentStep]), delay);
	}

	public void shutdown()
	{
		store();
		try
		{
			if(dropTask != null)
			{
				dropTask.cancel(false);
				dropTask = null;
			}
			if(expireTask != null)
			{
				expireTask.cancel(false);
				expireTask = null;
			}
			if(fieldSteps[currentStep] != null)
				fieldSteps[currentStep].stopTask();
		}
		catch(Exception e)
		{
		}
	}

	private void store()
	{
		Connection con = null;
		PreparedStatement stmt = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("REPLACE INTO field_cycle VALUES(?,?,?,?,?)");
			stmt.setInt(1, id);
			stmt.setLong(2, currentPoint);
			stmt.setInt(3, currentStep);
			stmt.setInt(4, (int) (lastStepChangeTime / 1000));
			stmt.setInt(5, (int) (lastPointChangeTime / 1000));
			stmt.execute();

			DbUtils.closeQuietly(con, stmt);
			lastStoreTime = System.currentTimeMillis();
		}
		catch(Exception e)
		{
			_log.info("FieldCycle: " + id + " step: " + currentStep + " point: " + currentPoint + " lastStepChange: " + lastStepChangeTime + " lastPointChange: " + lastPointChangeTime + " can't store data: " + e);
			e.printStackTrace();
		}
	}

	public long getLastStepChange()
	{
		return lastStepChangeTime;
	}

	public long getLastPointChange()
	{
		return lastPointChangeTime;
	}

	public FieldStep getCurrentStep()
	{
		if(currentStep < 0 || currentStep >= fieldSteps.length)
			return null;

		return fieldSteps[currentStep];
	}

	public boolean isInRestartRange(L2Player player)
	{
		if(currentStep < 0 || currentStep >= fieldSteps.length || player == null)
			return false;

		FieldStep fs = fieldSteps[currentStep];
		return fs != null && fs.isInRestartRange(player);
	}

	public Location getRestartPoint(L2Player player)
	{
		if(currentStep < 0 || currentStep >= fieldSteps.length || player == null)
			return null;

		FieldStep fs = fieldSteps[currentStep];

		if(fs != null)
			return fs.getRestartPoint(player);

		return null;
	}
}