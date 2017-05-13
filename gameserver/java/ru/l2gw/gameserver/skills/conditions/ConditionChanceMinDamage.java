package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 06.08.2010 14:14:29
 */
public class ConditionChanceMinDamage extends ConditionChance
{
	private final int _minDamage;
	public ConditionChanceMinDamage(int minDamage)
	{
		_minDamage = minDamage;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.value > _minDamage;
	}
}
