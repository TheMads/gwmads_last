package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 06.08.2010 14:17:56
 */
public class ConditionChanceOnDamage extends ConditionChance
{
	private final int _chanceMod;
	public ConditionChanceOnDamage(int chanceMod)
	{
		_chanceMod = chanceMod;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return Rnd.chance((int) (env.value / _chanceMod));
	}
}
