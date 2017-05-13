package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.math.Rnd;

public class ConditionGameChance extends Condition
{
	private final int _chance;

	ConditionGameChance(int chance)
	{
		_chance = chance;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return Rnd.chance(_chance);
	}
}
