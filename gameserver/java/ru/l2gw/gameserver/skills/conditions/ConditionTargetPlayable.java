package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.skills.Env;

public class ConditionTargetPlayable extends Condition
{
	private final boolean _flag;

	public ConditionTargetPlayable(boolean flag)
	{
		_flag = flag;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target instanceof L2Playable == _flag;
	}
}
