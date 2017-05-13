package ru.l2gw.gameserver.skills.effects;

import javolution.util.FastList;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.commons.math.Rnd;

import java.util.List;

/**
 * @author: rage
 * @date: 25.09.2009 15:41:09
 */
public class t_distrust extends t_effect
{
	L2Character mob;

	public t_distrust(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(!getEffected().isFearImmune() && getEffected().isMonster())
		{
			L2NpcInstance target = (L2NpcInstance) getEffected();
			List<L2NpcInstance> targets = new FastList<L2NpcInstance>();

			for(L2NpcInstance npc : target.getAroundFriends())
				if(npc != null && GeoEngine.canMoveToCoord(target.getX(), target.getY(), target.getZ(), npc.getX(), npc.getY(), npc.getZ(), npc.getReflection()))
					targets.add(npc);

			targets.remove(target);

			if(targets.size() > 0)
			{
				L2NpcInstance npc = targets.get(Rnd.get(targets.size()));
				target.startConfused();
				target.getAI().notifyEvent(CtrlEvent.EVT_MANIPULATION, npc, 1, getSkill());
				target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, npc);
				mob = npc;
			}
		}
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(mob != null && getEffected().isMonster())
		{
			getEffected().getAI().removeAttackDesire(mob);
			if(mob.isMonster())
				mob.getAI().removeAttackDesire(getEffected());
		}
		getEffected().stopConfused();
	}
}
