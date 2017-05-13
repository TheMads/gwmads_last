package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author: rage
 * Date: 21.07.2009 14:28:13
 */
public class t_charm_of_courage extends t_effect
{
	public t_charm_of_courage(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(getEffected().isAlikeDead())
			return;
		
		if(getEffected().isPlayer())
			((L2Player) getEffected()).setCharmOfCourage(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(getEffected().isPlayer())
			((L2Player) getEffected()).setCharmOfCourage(false);
	}

}
