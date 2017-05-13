package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;

final class t_bless_noblesse extends t_effect
{
	public t_bless_noblesse(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		getEffected().setIsBlessedByNoblesse(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().setIsBlessedByNoblesse(false);
	}
}