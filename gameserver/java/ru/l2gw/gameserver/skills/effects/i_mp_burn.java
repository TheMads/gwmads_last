package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.arrays.GArray;

/**
 * User: ic
 * Date: 22.04.2010
 */
public class i_mp_burn extends i_effect
{
	public i_mp_burn(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target == null || env.target.isDead())
				continue;

			double damage = Formulas.calcManaDam(cha, env.target, getSkill(), ss);
			boolean crit = Formulas.calcMCrit(cha.getCriticalMagic(env.target, getSkill()));
			if(crit)
				cha.sendPacket(Msg.MAGIC_CRITICAL_HIT);

			if(env.target.isStatActive(Stats.BLOCK_MP))
			{
				damage = 0;
				cha.sendPacket(Msg.THE_ATTACK_HAS_BEEN_BLOCKED);
			}

			env.target.reduceCurrentMp(crit ? damage * 3 : damage, cha);
		}
	}
}
