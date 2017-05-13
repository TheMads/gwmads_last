package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 02.07.2009 12:54:12
 */
public class i_damage_random extends i_effect
{
	public i_damage_random(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target != null && !env.target.isDead())
				env.target.reduceHp(Rnd.get(env.target.getMaxHp()), cha, true, false);
	}
}
