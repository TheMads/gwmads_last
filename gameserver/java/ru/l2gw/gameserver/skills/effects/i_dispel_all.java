package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author ic
 * @date 23.12.2009
 */
public class i_dispel_all extends i_effect
{
	public i_dispel_all(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target == null || env.target.isDead())
				continue;

			if(env.target instanceof L2Playable)
				((L2Playable) env.target).setMassUpdating(true);
			for(L2Effect e : env.target.getAllEffects())
				if(e != null && !e.getSkill().isToggle())
					env.target.stopEffect(e.getSkillId());
			if(env.target instanceof L2Playable)
			{
				((L2Playable) env.target).setMassUpdating(false);
				env.target.updateEffectIcons();
				env.target.sendChanges();
			}
		}
	}
}
