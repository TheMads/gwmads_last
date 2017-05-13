package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 11.09.2009 17:37:12
 */
public class i_frint_abort extends i_effect
{
	public i_frint_abort(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		/*
		for(Env env : targets)
			if(env.target instanceof L2FrintezzaInstance)
				((L2FrintezzaInstance) env.target).abortMelody();
		*/
	}
}
