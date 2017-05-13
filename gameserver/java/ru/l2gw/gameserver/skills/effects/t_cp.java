package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author: rage
 * @date: 17.07.2010 22:34:42
 */
public class t_cp extends t_effect
{
	public t_cp(L2Effect effect, EffectTemplate template)
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

		double cp = calcTickVal();
		if(cp > 0)
		{
			int cpLimit = (int) getEffected().calcStat(Stats.CP_LIMIT, getEffected().getMaxCp(), null, null);
			double newCp = cp;

			if(getEffected().getCurrentHp() + newCp > cpLimit)
				newCp = Math.max(0, cpLimit - getEffected().getCurrentCp());

			getEffected().setCurrentCp(getEffected().getCurrentCp() + newCp);
		}
		else
		{
			if(getEffected().isStatActive(Stats.BLOCK_HP))
				return true;

			getEffected().setCurrentCp(getEffected().getCurrentCp() + cp);
		}
		return true;
	}
}
