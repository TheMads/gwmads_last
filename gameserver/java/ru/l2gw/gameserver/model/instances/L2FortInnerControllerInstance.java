package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * @author rage
 * @date 15.07.2009 9:20:21
 */
public class L2FortInnerControllerInstance extends L2FortPowerControllerInstance
{
	public L2FortInnerControllerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	public void onSpawn()
	{
		super.onSpawn();
		_file =  "inner_controller";
		PASS_LEN = 2;
	}
 }
