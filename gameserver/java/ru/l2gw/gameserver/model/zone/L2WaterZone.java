package ru.l2gw.gameserver.model.zone;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2ObjectTasks.MoveToTask;

public class L2WaterZone extends L2DefaultZone
{
	public L2WaterZone()
	{
		super();
		_zoneTypes.add(ZoneType.water);
	}

	@Override
	public void onEnter(L2Character cha)
	{
		super.onEnter(cha);
		if(cha.isPlayer())
		{
			cha.getPlayer().startWaterTask();
			if(cha.isMoving)
			{
				cha.isMoving = false;
				ThreadPoolManager.getInstance().executeMove(new MoveToTask(cha, cha.getDesireLoc()));
			}
		}
	}

	@Override
	public void onExit(L2Character cha)
	{
		super.onExit(cha);
		if(cha.isPlayer())
		{
			cha.getPlayer().stopWaterTask();
			if(cha.isMoving)
			{
				cha.isMoving = false;
				ThreadPoolManager.getInstance().executeMove(new MoveToTask(cha, cha.getDesireLoc()));
			}
		}
	}
}
