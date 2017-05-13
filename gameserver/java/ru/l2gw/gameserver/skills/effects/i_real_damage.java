package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author admin
 * @date 27.11.2010 15:14:50
 */
public class i_real_damage extends i_effect
{
	public i_real_damage(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target == null)
				continue;

			double damage = calc();

			if(damage < 1)
				damage = 1;

			boolean blockHp = env.target.isStatActive(Stats.BLOCK_HP);
			if(blockHp)
				damage = 0;

			env.target.reduceHp(damage, cha, false, false);
			cha.sendDamageMessage(env.target, (int) damage, false, false, blockHp);
		}
	}
}
