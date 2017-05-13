package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 23.11.2009 13:44:17
 */
public class i_consume_body extends i_effect
{
	public i_consume_body(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target instanceof L2NpcInstance && env.target.isDead())
				((L2NpcInstance) env.target).endDecayTask();
	}
}
