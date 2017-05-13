package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public final class ConditionUsingSkill extends Condition
{
	private final int _skillId;

	public ConditionUsingSkill(int skillId)
	{
		_skillId = skillId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.skill != null && env.skill.getId() == _skillId;
	}
}