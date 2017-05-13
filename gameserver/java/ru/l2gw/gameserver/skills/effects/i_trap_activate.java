package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2TrapInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 27.11.2009 15:22:46
 */
public class i_trap_activate extends i_effect
{
	public i_trap_activate(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target instanceof L2TrapInstance)
				((L2TrapInstance) env.target).setActive(true);
	}
}
