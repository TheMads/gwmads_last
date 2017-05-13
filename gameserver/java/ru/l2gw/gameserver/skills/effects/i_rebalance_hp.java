package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 10.02.2010 13:56:38
 */
public class i_rebalance_hp extends i_effect
{
	public i_rebalance_hp(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		double summaryCurrentHp = 0;
		int summaryMaximumHp = 0;

		for(Env env : targets)
			if(!env.target.isDead())
			{
				summaryCurrentHp += env.target.getCurrentHp();
				summaryMaximumHp += env.target.getMaxHp();
			}

		double percent = summaryCurrentHp / summaryMaximumHp;

		for(Env env : targets)
			if(!env.target.isDead())
			{
				int hpLimit = (int) env.target.calcStat(Stats.HP_LIMIT, env.target.getMaxHp(), null, null);
				int newHp = (int) (env.target.getMaxHp() * percent);

				if(newHp > env.target.getCurrentHp() && newHp > hpLimit)
					newHp = hpLimit < env.target.getCurrentHp() ? (int) env.target.getCurrentHp() : hpLimit;

				env.target.setCurrentHp(newHp);
			}
	}
}
