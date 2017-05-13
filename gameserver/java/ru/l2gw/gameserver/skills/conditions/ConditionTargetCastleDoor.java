package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.skills.Env;

public class ConditionTargetCastleDoor extends Condition
{
	private final boolean _isCastleDoor;

	public ConditionTargetCastleDoor(boolean isCastleDoor)
	{
		_isCastleDoor = isCastleDoor;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target instanceof L2DoorInstance == _isCastleDoor;
	}
}
