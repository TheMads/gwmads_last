package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Effect;

/**
 * @author: rage
 * @date: 17.07.2010 22:31:59
 */
public class c_mp_by_level extends t_effect
{
	public c_mp_by_level(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public double calcTickVal()
	{
		int level = getEffected().getLevel();
		return (level * 0.0027 * level - level * 0.00002 * level * level + level * 0.0203 + 1.2171) * _template._val * _template._ticks * 666 / 1000.;
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

		double mp = getEffected().getCurrentMp() + calcTickVal();
		if(mp <= 0)
		{
			getEffected().sendPacket(Msg.NOT_ENOUGH_MP);
			getEffected().setCurrentMp(0);
			return false;
		}
		getEffected().setCurrentMp(mp);
		return true;
	}
}
