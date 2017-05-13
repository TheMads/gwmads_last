package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 31.12.2009 13:32:57
 */
public class i_p_attack_hp_link extends i_effect
{
	public i_p_attack_hp_link(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target == null || Formulas.calcDodge(cha, env.target, getSkill()))
				continue;

			if(!counter)
				env.target.doCounterAttack(getSkill(), cha);

			double damage = Formulas.calcPhysDam(cha, env, getSkill(), ss > 0);

			boolean block = env.target.isStatActive(Stats.BLOCK_HP);
			if(block)
				damage = 0;

			L2Character reflector = null;

			if(getSkill().getCastRange() <= 100 && env.target.checkReflectMeleeSkill(getSkill()))
			{
				reflector = env.target;
				env.target = cha.isCubic() ? cha.getPlayer() : cha;

				if(env.target.isStatActive(Stats.BLOCK_HP))
				{
					block = true;
					damage = 0;
				}
			}

			if(damage > 1 && getSkill().getPower(cha, env.target) > 0)
			{
				damage *= 1.8 * (1. - cha.getCurrentHp() / cha.getMaxHp());

				if(reflector != null)
					reflector.sendDamageMessage(env.target, (int) damage, false, false, block);
				else
					cha.sendDamageMessage(env.target, (int) damage, false, false, block);
			}

			if(!block && damage < 1)
				damage = 1;

			if(getSkill().getPower(cha, env.target) > 0)
				env.target.reduceHp(damage, reflector != null ? reflector : cha, false, false);
		}
	}
}
