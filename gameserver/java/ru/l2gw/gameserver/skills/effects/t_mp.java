package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author: rage
 * @date: 17.07.2010 22:23:18
 */
public class t_mp extends t_effect
{
	public t_mp(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		startActionTask(3000);
	}

	@Override
	public boolean onActionTime()
	{
		if(getEffected().isDead())
			return false;

		double mp = calcTickVal();
		if(mp > 0)
		{
			int mpLimit = (int) getEffected().calcStat(Stats.MP_LIMIT, getEffected().getMaxMp(), null, null);
			int newMp = (int) mp;

			if(getEffected().getCurrentMp() + newMp > mpLimit)
				newMp = (int) Math.max(0, mpLimit - getEffected().getCurrentMp());

			getEffected().setCurrentMp(getEffected().getCurrentMp() + newMp);
		}
		else
		{
			if(getEffected().isStatActive(Stats.BLOCK_MP))
				return true;

			getEffected().reduceCurrentMp(-mp, getEffector());
		}
		return true;
	}
}
