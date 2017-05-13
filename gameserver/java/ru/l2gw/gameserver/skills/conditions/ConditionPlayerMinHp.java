package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public class ConditionPlayerMinHp extends Condition
{
	private final float _hp;

	public ConditionPlayerMinHp(int hp)
	{
		_hp = hp;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getCurrentHp() > _hp;
	}
}