package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;

/**
 * User: agr0naft
 * Date: 23.01.2011
 */
public class t_hourglass extends t_effect
{
	public t_hourglass(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(!getEffected().isPlayer())
			return;

		L2Player player = (L2Player) getEffected();
		player.startHourglassEffect();
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(!getEffected().isPlayer())
			return;

		L2Player player = (L2Player) getEffected();
		player.stopHourglassEffect();
	}
}
