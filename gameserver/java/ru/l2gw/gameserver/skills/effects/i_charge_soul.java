package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 14.07.2010 13:39:22
 */
public class i_charge_soul extends i_effect
{
	public i_charge_soul(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(!env.target.isPlayer() || env.target.isDead())
				continue;

			env.target.increaseSouls((int) calc());
		}
	}
}
