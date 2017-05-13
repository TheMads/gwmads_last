package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;

final class t_salvation extends t_effect
{

	public t_salvation(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		getEffected().setIsSalvation(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().setIsSalvation(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}