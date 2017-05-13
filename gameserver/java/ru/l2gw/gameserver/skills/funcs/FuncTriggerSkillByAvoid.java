package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author: rage
 * @date: 26.10.11 21:32
 */
public class FuncTriggerSkillByAvoid extends FuncTriggerSkill
{
	public FuncTriggerSkillByAvoid(Stats stat, int order, Object owner, double value)
	{
		super(Stats.TRIGGER_BY_AVOID, order, owner, value);
	}

	@Override
	public void calc(Env env)
	{
		if(debug)
			_log.info("trigger_skill_by_avoid: " + triggerSkill);

		if(triggerSkill == null)
			return;

		if(env.character.isAlikeDead() || env.character.isParalyzed() || env.target == null || env.target.isDead())
			return;

		if(_cond == null || _cond.test(env))
	    	super.calc(env);
	}
}
