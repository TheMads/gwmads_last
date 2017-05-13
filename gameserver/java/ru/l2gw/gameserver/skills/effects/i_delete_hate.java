package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 24.09.2009 13:39:15
 */
public class i_delete_hate extends i_effect
{
	public i_delete_hate(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(!(env.target instanceof L2NpcInstance) || env.target.isRaid() || !env.success || (_template._activateRate > 0 && !Rnd.chance(_template._activateRate)))
				continue;

			L2NpcInstance npc = (L2NpcInstance) env.target;

			npc.stopHate();
			npc.setAttackTimeout(Integer.MAX_VALUE);
			npc.setTarget(null);
			npc.getAI().setGlobalAggro(-10);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
	}
}
