package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * @author: rage
 * @date: 26.10.11 20:17
 */
public class FuncTriggerSkillByAttack extends FuncTriggerSkillByDmg
{
	private boolean crit;

	public FuncTriggerSkillByAttack(Stats stat, int order, Object owner, double value)
	{
		super(Stats.TRIGGER_BY_ATTACK, order, owner, value);
	}

	@Override
	public void setAttributes(StatsSet set)
	{
		super.setAttributes(set);
		crit = attrs.getBool("onCrit", false);
	}

	@Override
	public void calc(Env env)
	{
		if(debug)
			_log.info("trigger_skill_by_attack: " + triggerSkill);

		if(triggerSkill == null)
			return;

		if(env.character.isAlikeDead() || env.character.isParalyzed() || env.target == null || env.target.isDead() || env.value < minDamage || env.target.getLevel() < minLevel || env.target.getLevel() > maxLevel || env.success != crit)
			return;

		if(_cond != null && !_cond.test(env))
			return;

		if(target == TriggerTarget.enemy_all || target == TriggerTarget.pc && env.target.isPlayer() || target == TriggerTarget.mob && env.target.isNpc())
			super.calc(env);
	}
}
