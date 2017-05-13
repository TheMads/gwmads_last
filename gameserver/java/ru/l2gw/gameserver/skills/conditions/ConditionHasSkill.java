package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public final class ConditionHasSkill extends Condition
{
	private final Integer _id;
	private final short _level;

	public ConditionHasSkill(Integer id, short level)
	{
		_id = id;
		_level = level;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.skill != null && env.character.getSkillLevel(_id) >= _level;
	}
}
