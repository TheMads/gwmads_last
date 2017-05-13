package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.instances.L2ChestInstance;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 18.11.2009 16:39:19
 */
public class ConditionTargetUnlockable extends Condition
{
	private final boolean _unlockable;

	public ConditionTargetUnlockable(boolean unlockable)
	{
		_unlockable = unlockable;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return (env.target instanceof L2ChestInstance) == _unlockable || (env.target instanceof L2DoorInstance && ((L2DoorInstance) env.target).isUnlockable() == _unlockable); 
	}

}
