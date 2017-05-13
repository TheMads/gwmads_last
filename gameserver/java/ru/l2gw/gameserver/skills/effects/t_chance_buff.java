package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;

/**
 * User: rage
 * Date: 18.11.2008
 * Time: 14:59:54
 */
public class t_chance_buff extends t_effect
{
	public t_chance_buff(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(getEffected().isPlayer())
			getEffected().addChanceSkill(getSkill());
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(getEffected().isPlayer())
			getEffected().removeChanceSkill(getSkill().getId());
	}
}
