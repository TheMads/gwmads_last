package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 24.09.2009 14:39:46
 */
public class i_add_hate extends i_effect
{
	public i_add_hate(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target instanceof L2NpcInstance)
			{
				L2NpcInstance npc = (L2NpcInstance) env.target;

				int hate = (int) calc();
				if(hate > 0)
					npc.getAI().notifyEvent(CtrlEvent.EVT_MANIPULATION, cha, hate, getSkill());
				else
					npc.addDamageHate(cha, 0, hate);
			}
	}
}
