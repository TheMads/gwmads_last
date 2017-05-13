package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author rage
 * @date 08.10.2009 15:32:16
 */
public class i_p_attack extends i_effect
{
	public i_p_attack(EffectTemplate template)
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

			if(damage < 1)
				damage = 1;

			boolean blockHp = env.target.isStatActive(Stats.BLOCK_HP);
			if(blockHp)
				damage = 0;

			L2Character reflector = null;

			if(getSkill().getCastRange() <= 100 && env.target.checkReflectMeleeSkill(getSkill()))
			{
				reflector = env.target;
				env.target = cha.isCubic() ? cha.getPlayer() : cha;

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
