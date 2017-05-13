package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

/**
 * @author: rage
 * @date: 14.07.2010 13:00:52
 */
public class i_erase_summon extends i_effect
{
	public i_erase_summon(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target.isSummon() && (getSkill().getActivateRate() <= 0 || Rnd.chance(getSkill().getActivateRate())))
				((L2Summon) env.target).unSummon();
	}
}
