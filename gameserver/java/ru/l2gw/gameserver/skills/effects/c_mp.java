package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Effect;

/**
 * @author: rage
 * @date: 17.07.2010 22:29:45
 */
public class c_mp extends t_effect
{
	public c_mp(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

/*
	@Override
	public EffectType getEffectType()
	{
		return EffectType.continuous;
	}
*/

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
