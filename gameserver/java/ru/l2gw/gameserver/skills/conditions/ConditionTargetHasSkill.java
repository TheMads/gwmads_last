package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public class ConditionTargetHasSkill extends Condition
{

	private final int _skillId, _skillLevel;

	public ConditionTargetHasSkill(int skillId, int skillLevel)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target.getSkillLevel(_skillId) > _skillLevel;
	}
}
