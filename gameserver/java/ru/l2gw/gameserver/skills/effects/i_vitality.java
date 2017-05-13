package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * User: ic
 * Date: 28.04.2010
 */
public class i_vitality extends i_effect
{
	public i_vitality(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target != null && !env.target.isDead() && env.target.isPlayer())
			{
				L2Player player = (L2Player) env.target;
				player.getVitality().addPoints((int) calc());
			}
	}
}
