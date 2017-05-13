package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * @author: rage
 * @date: 26.10.11 17:09
 */
public class FuncTriggerSkillByDmg extends FuncTriggerSkill
{
	protected int minDamage;
	protected int minLevel;
	protected int maxLevel;

	public FuncTriggerSkillByDmg(Stats stat, int order, Object owner, double value)
	{
		super(stat == null ? Stats.TRIGGER_BY_DMG : stat, order, owner, value);
	}

	@Override
	public void setAttributes(StatsSet set)
	{
		super.setAttributes(set);
		minDamage = attrs.getInteger("minDmg", 1);
		String targetInfo[] = attrs.getString("attacker", "enemy_all;1;100").split(";");
		target = TriggerTarget.valueOf(targetInfo[0]);
		minLevel = Integer.parseInt(targetInfo[1]);
		maxLevel = Integer.parseInt(targetInfo[2]);
	}

	@Override
	public void calc(Env env)
	{
		if(debug)
			_log.info("trigger_skill_by_dmg: " + triggerSkill + " " + env.character + " -> " + env.target + " chance: " + chance);

		if(triggerSkill == null)
			return;

		if(env.character.isAlikeDead() || env.character.isParalyzed() || env.target == null || env.target.isDead() || env.value < minDamage || env.target.getLevel() < minLevel || env.target.getLevel() > maxLevel)
			return;

		if(_cond != null && !_cond.test(env))
			return;

		if(target == TriggerTarget.enemy_all || target == TriggerTarget.pc && env.target.isPlayer() || target == TriggerTarget.mob && env.target.isNpc())
			super.calc(env);
	}
}
