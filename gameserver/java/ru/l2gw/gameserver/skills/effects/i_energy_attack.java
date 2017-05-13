package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author rage
 * @date 31.12.2009 12:03:08
 */
public class i_energy_attack extends i_effect
{
	public i_energy_attack(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target.isDead() || Formulas.calcDodge(cha, env.target, getSkill()))
				continue;

			if(!counter)
				env.target.doCounterAttack(getSkill(), cha);

			double damage = Formulas.calcEnergyDam(cha, env, getSkill(), ss > 0);

			if(damage < 1)
				damage = 1;

			boolean blockHp = env.target.isStatActive(Stats.BLOCK_HP);
			if(blockHp)
				damage = 0;

			L2Character reflector = null;

			if(getSkill().getCastRange() <= 100 && env.target.checkReflectMeleeSkill(getSkill()))
			{
				reflector = env.target;
				env.target = cha;
				if(env.target.isStatActive(Stats.BLOCK_HP))
					damage = 0;
			}

			env.target.reduceHp(damage, reflector != null ? reflector : cha, false, false);

			if(reflector != null)
				reflector.sendDamageMessage(env.target, (int) damage, false, env.crit, blockHp);
			else
				cha.sendDamageMessage(env.target, (int) damage, false, env.crit, blockHp);
		}
	}
}
