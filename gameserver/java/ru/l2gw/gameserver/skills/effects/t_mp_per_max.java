package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author: rage
 * @date: 12.09.11 3:53
 */
public class t_mp_per_max extends t_effect
{
	public t_mp_per_max(L2Effect effect, EffectTemplate template)
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

		if(mp > 0) // heal
		{
			int mpLimit = (int) getEffected().calcStat(Stats.MP_LIMIT, getEffected().getMaxMp(), null, null);
			int newMp = (int) (getEffected().getMaxMp() * mp * 0.01);

			if(getEffected().getCurrentMp() + newMp > mpLimit)
				newMp = (int) Math.max(0, mpLimit - getEffected().getCurrentHp());

			getEffected().setCurrentMp(getEffected().getCurrentMp() + newMp);
			return true;
		}
		else // dot
		{
			if(getEffected().isStatActive(Stats.BLOCK_MP) || getEffected().isInvul())
				return true;

			double damage = -getEffected().getMaxMp() * mp * 0.01;
			getEffected().reduceCurrentMp(damage, getEffector());
		}
		return true;
	}
}
