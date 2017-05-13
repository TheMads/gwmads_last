package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 01.06.2009 12:16:44
 */
public class ConditionPlayerHasEffect extends Condition
{
	private final int _skillId;
	private final int _skillLevel;

	public ConditionPlayerHasEffect(int skillId, int skillLevel)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getEffectBySkillId(_skillId) != null && env.character.getEffectBySkillId(_skillId).getSkillLevel() >= _skillLevel;
	}
}
