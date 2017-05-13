package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 13.07.2010 15:36:18
 */
public class i_remove_dp extends i_effect
{
	public i_remove_dp(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target.isPlayer())
				((L2Player) env.target).getDeathPenalty().reduceLevel();
	}
}
