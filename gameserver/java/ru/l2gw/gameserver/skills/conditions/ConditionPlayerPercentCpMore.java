package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public class ConditionPlayerPercentCpMore extends Condition
{
	private final float _cp;

	public ConditionPlayerPercentCpMore(int cp)
	{
		_cp = cp / 100f;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getCurrentCp() >= _cp * env.character.getMaxCp();
	}
}