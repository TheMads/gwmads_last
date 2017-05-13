package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 03.08.2010 14:25:41
 */
public class ConditionTargetPet extends Condition
{
	private final boolean _flag;

	public ConditionTargetPet(boolean flag)
	{
		_flag = flag;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target instanceof L2PetInstance == _flag;
	}
}
