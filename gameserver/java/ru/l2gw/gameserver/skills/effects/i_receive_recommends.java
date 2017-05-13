package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * User: agr0naft
 * Date: 23.01.2011
 */
public class i_receive_recommends extends i_effect
{
	public i_receive_recommends(EffectTemplate template)
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
				player.getRecSystem().addRecommendsHave((int) calc());
			}
	}
}
