package ru.l2gw.gameserver.model.entity.fieldcycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Territory;
import ru.l2gw.gameserver.model.entity.fieldcycle.FieldTasks.StepIntervalTask;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.util.Location;
import ru.l2gw.commons.crontab.Crontab;

import java.util.concurrent.ScheduledFuture;

/**
 * @author: rage
 * @date: 11.12.11 16:50
 */
public class FieldStep
{
	private static final Log _log = LogFactory.getLog("fieldcycle");
	private final int filedId;
	private final int step;
	private final long stepPoint;
	private final long lockTime;
	private final long dropTime;
	private final long intervalTime;
	private final long intervalPoint;
	private Crontab changeTime;
	private L2Zone[] areaOn;
	private L2DoorInstance[] openDoor;
	private L2Territory restartRange;
	private Location[] normalPoints;
	private Location[] chaoPoints;
	private int mapString;
	private Location mapLoc;
	private ScheduledFuture<?> intervalTask;

	public FieldStep(int fieldId, int step, long stepPoint, long lockTime, long dropTime, long intervalTime, long intervalPoint)
	{
		this.filedId = fieldId;
		this.step = step;
		this.stepPoint = stepPoint;
		this.lockTime = lockTime * 1000;
		this.dropTime = dropTime * 1000;
		this.intervalTime = intervalTime;
		this.intervalPoint = intervalPoint;
	}

	public int getFieldId()
	{
		return filedId;
	}

	public int getStep()
	{
		return step;
	}

	public long getPoint()
	{
		return stepPoint;
	}

	public long getLockTime()
	{
		return lockTime;
	}

	public long getIntervalTime()
	{
		return intervalTime;
	}

	public long getIntervalPoint()
	{
		return intervalPoint;
	}

	public long getDropTime()
	{
		return dropTime;
	}

	public long getExpireTime()
	{
		if(changeTime == null)
			return 0;

		return changeTime.timeNextUsage(System.currentTimeMillis());
	}

	public void stop()
	{
		if(areaOn != null)
		{
			for(L2Zone zone : areaOn)
				zone.setActive(false);
		}
		if(openDoor != null)
		{
			for(L2DoorInstance door : openDoor)
				door.closeMe();
		}
		if(intervalTask != null)
		{
			intervalTask.cancel(false);
			intervalTask = null;
		}
	}

	public void start()
	{
		if(areaOn != null)
		{
			for(L2Zone zone : areaOn)
				zone.setActive(true);
		}
		if(openDoor != null)
		{
			for(L2DoorInstance door : openDoor)
			{
				door.openMe();
				door.onOpen();
			}
		}

		if(intervalTime > 0 && intervalPoint != 0)
		{
			if(intervalTask != null)
				intervalTask.cancel(false);

			intervalTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new StepIntervalTask(this), intervalTime, intervalTime);
		}
	}

	public void addAreaOn(L2Zone zone)
	{
		if(areaOn == null)
			areaOn = new L2Zone[]{ zone };
		else
		{
			int len = areaOn.length;
			L2Zone[] tmp = new L2Zone[len + 1];
			System.arraycopy(areaOn, 0, tmp, 0, len);
			tmp[len] = zone;
			areaOn = tmp;
		}
	}

	public void addOpenDoor(L2DoorInstance door)
	{
		if(openDoor == null)
			openDoor = new L2DoorInstance[]{ door };
		else
		{
			int len = openDoor.length;
			L2DoorInstance[] tmp = new L2DoorInstance[len + 1];
			System.arraycopy(openDoor, 0, tmp, 0, len);
			tmp[len] = door;
			openDoor = tmp;
		}
	}

	public void addNormalPoint(Location loc)
	{
		if(normalPoints == null)
			normalPoints = new Location[]{ loc };
		else
		{
			int len = normalPoints.length;
			Location[] tmp = new Location[len + 1];
			System.arraycopy(normalPoints, 0, tmp, 0, len);
			tmp[len] = loc;
			normalPoints = tmp;
		}
	}

	public void addChaoPoint(Location loc)
	{
		if(chaoPoints == null)
			chaoPoints = new Location[]{ loc };
		else
		{
			int len = chaoPoints.length;
			Location[] tmp = new Location[len + 1];
			System.arraycopy(chaoPoints, 0, tmp, 0, len);
			tmp[len] = loc;
			chaoPoints = tmp;
		}
	}

	public void setChangeTime(String time)
	{
		changeTime = new Crontab(time);
	}

	public void setRestartRange(L2Territory terr)
	{
		restartRange = terr;
	}

	public void setMapString(String loc, int stringId)
	{
		mapLoc = Location.parseLoc(loc);
		mapString = stringId;
	}

	public void stopTask()
	{
		if(intervalTask != null)
		{
			intervalTask.cancel(false);
			intervalTask = null;
		}
	}

	public int getMapString()
	{
		return mapString;
	}

	public Location getMapLoc()
	{
		return mapLoc;
	}

	public boolean isInRestartRange(L2Player player)
	{
		return restartRange != null && player != null && restartRange.isInside(player.getX(), player.getY(), player.getZ());
	}

	public Location getRestartPoint(L2Player player)
	{
		if(player == null)
			return null;

		if(player.getKarma() > 0)
		{
			if(chaoPoints != null && chaoPoints.length > 0)
				return chaoPoints[Rnd.get(chaoPoints.length)];
		}
		else if(normalPoints != null && normalPoints.length > 0)
			return normalPoints[Rnd.get(normalPoints.length)];

		return null;
	}
}
