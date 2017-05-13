package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Effect;

/**
 * @author: rage
 * @date: 17.07.2010 21:54:16
 */
public class c_hp extends t_effect
{
	public c_hp(L2Effect effect, EffectTemplate template)
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

		double hp = getEffected().getCurrentHp() + calcTickVal();
		if(hp <= 1)
		{
			getEffected().sendPacket(Msg.YOUR_SKILL_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_HP);
			getEffected().setCurrentHp(1);
			return false;
		}
		getEffected().setCurrentHp(hp);
		return true;
	}
}
