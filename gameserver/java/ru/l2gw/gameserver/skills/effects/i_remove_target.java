package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

public class i_remove_target extends i_effect
{
	public i_remove_target(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target != null && !env.target.isRaid() && env.success && (_template._activateRate <= 0 || Rnd.chance(_template._activateRate)) && !env.target.isStatActive(Stats.BLOCK_DEBUFF))
			{
				if(env.target.isNpc())
				{
					L2NpcInstance npc = (L2NpcInstance) env.target;
					npc.getAI().setGlobalAggro(-3);
					npc.setTarget(null);
					npc.stopMove();
					npc.abortAttack();
					npc.abortCast();
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				}
				else
				{
					env.target.setTarget(null);
					//env.target.stopMove();
					env.target.abortAttack();
					env.target.abortCast();
		
					if(env.target.isAttackingNow())
					{
						env.target.abortAttack();
						env.target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					}
					else if(env.target.isCastingNow())
						env.target.abortCast();
				}
			}
	}
}