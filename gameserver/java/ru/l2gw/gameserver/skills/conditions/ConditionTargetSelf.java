package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 26.07.2010 16:34:56
 */
public class ConditionTargetSelf extends Condition
{
	private final boolean _flag;

	public ConditionTargetSelf(boolean flag)
	{
		_flag = flag;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return (env.target == env.character) == _flag;
	}
}
