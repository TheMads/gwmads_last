package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * @author: rage
 * @date: 26.10.11 21:17
 */
public class FuncTriggerSkillBySkill extends FuncTriggerSkill
{
	private int skillId;

	public FuncTriggerSkillBySkill(Stats stat, int order, Object owner, double value)
	{
		super(Stats.TRIGGER_BY_SKILL, order, owner, value);
	}

	@Override
	public void setAttributes(StatsSet set)
	{
		super.setAttributes(set);
		skillId = attrs.getInteger("onSkillUse");
	}

	@Override
	public void calc(Env env)
	{
		if(debug)
			_log.info("trigger_skill_by_skill: " + triggerSkill);

		if(env.skill == null || triggerSkill == null)
			return;

		if(skillId <= 0)
		{
			if(skillId == 0 && !env.skill.isPhysic())
				return;
			if(skillId == -1 && !env.skill.isMagic())
				return;
			if(skillId == -2 && (!env.skill.isMagic() || !env.skill.isOffensive()))
				return;
		}
		else if(skillId != env.skill.getId())
			return;

		if(env.character.isAlikeDead() || env.character.isParalyzed() || env.target == null || env.target.isDead())
			return;

		if(_cond != null && !_cond.test(env))
			return;

	    super.calc(env);
	}
}
