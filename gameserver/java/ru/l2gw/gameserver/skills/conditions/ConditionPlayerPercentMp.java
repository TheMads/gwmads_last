package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public class ConditionPlayerPercentMp extends Condition
{
	private final float _mp;

	public ConditionPlayerPercentMp(int mp)
	{
		_mp = mp / 100f;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getCurrentMp() <= _mp * env.character.getMaxMp();
	}
}