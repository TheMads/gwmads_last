package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

/**
 * @author: rage
 * @date: 25.09.2009 15:17:38
 */
public class t_confusion extends t_effect
{
	L2Character mob = null;

	public t_confusion(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(!getEffected().isFearImmune() && (getEffected().isMonster() || getEffected() instanceof L2Summon))
		{
			if(getEffected().isMonster())
			{
				L2NpcInstance target = (L2NpcInstance) getEffected();
				GArray<L2Character> targets = new GArray<L2Character>();

				for(L2Character cha : target.getKnownCharacters(getSkill().getSkillRadius()))
					if(cha != null && !cha.isDead() && GeoEngine.canMoveToCoord(target.getX(), target.getY(), target.getZ(), cha.getX(), cha.getY(), cha.getZ(), target.getReflection()))
						targets.add(cha);

				targets.remove(getEffector());
				
				if(targets.size() > 0)
				{
					L2Character cha = targets.get(Rnd.get(targets.size()));
					target.startConfused();
					target.getAI().notifyEvent(CtrlEvent.EVT_MANIPULATION, cha, 1, getSkill());
					target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, cha);
					mob = cha;
				}
			}
			else if(getEffected() instanceof L2Summon)
			{
				L2Playable target = (L2Playable) getEffected();

				GArray<L2Character> targets = new GArray<L2Character>();

				for(L2Character cha : target.getKnownCharacters(getSkill().getSkillRadius()))
					if(cha != null && !cha.isDead() && cha.isAttackable(target, false, false) && GeoEngine.canMoveToCoord(target.getX(), target.getY(), target.getZ(), cha.getX(), cha.getY(), cha.getZ(), target.getReflection()))
						targets.add(cha);

				if(targets.size() > 0)
				{
					L2Character cha = targets.get(Rnd.get(targets.size()));
					target.startConfused();
					target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, cha);
					mob = cha;
				}
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
