package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.conditions.ConditionCanResurrect;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author admin
 * @date 03.08.2010 14:06:18
 */
public class i_resurrection extends i_effect
{
	public i_resurrection(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		int wit = cha.getWIT();
		double percent = 0.14 * wit * wit - 0.67 * wit - 43.;

		percent = Math.min(calc() * (1. + percent / 100.) - calc(), 20);
		percent = Math.min(percent + calc(), 100);

		for(Env env : targets)
		{
			if(env.target == cha || !(env.target instanceof L2Playable))
				continue;

			L2Playable target = (L2Playable) env.target;

			if(ConditionCanResurrect.checkSiegeCond(cha, target) != null)
				continue;

			if(!target.isDead() || target.getPlayer() == null || target.getPlayer().isInOlympiadMode() || target.getPlayer().inObserverMode())
				continue;

			if(target.isPet())
				target.getPlayer().reviveRequest((L2Player) cha, percent, true, false);
			else if(target.isPlayer())
			{
				L2Player targetPlayer = (L2Player) target;

				if(targetPlayer.isReviveRequested())
					continue;

				targetPlayer.reviveRequest((L2Player) cha, percent, false, false);
			}
		}
	}
}
