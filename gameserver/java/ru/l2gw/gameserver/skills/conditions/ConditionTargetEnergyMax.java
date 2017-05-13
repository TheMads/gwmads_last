package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 31.12.2009 13:07:42
 */
public class ConditionTargetEnergyMax extends Condition
{
	private final int _max;

	public ConditionTargetEnergyMax(int max)
	{
		_max = max;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target.isPlayer() && env.target.getPlayer().getIncreasedForce() < _max;
	}

}
