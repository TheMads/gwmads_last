package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public class ConditionPlayerPercentHp extends Condition
{
	private final float _hp;

	public ConditionPlayerPercentHp(int hp)
	{
		_hp = hp / 100f;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getCurrentHp() <= _hp * env.character.getMaxHp();
	}
}