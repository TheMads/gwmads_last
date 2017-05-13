package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 06.08.2010 11:44:00
 */
public class ConditionChanceOnSkillUse extends ConditionChance
{
	private final int _skillId;

	public ConditionChanceOnSkillUse(int skillId)
	{
		_skillId = skillId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.skill != null && env.skill.getId() == _skillId;
	}
}
