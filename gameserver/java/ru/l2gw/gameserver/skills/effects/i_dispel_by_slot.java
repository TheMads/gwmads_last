package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 23.11.2009 13:29:14
 */
public class i_dispel_by_slot extends i_effect
{
	private final String slotType;
	private final int maxAbnormalLevel;

	public i_dispel_by_slot(EffectTemplate template)
	{
		super(template);
		slotType = template._attrs.getString("slotType", "");
		maxAbnormalLevel = template._attrs.getInteger("maxAbnormalLevel", 0);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(slotType.equals(""))
			return;

		for(Env env : targets)
		{
			if(env.target == null || env.target.isDead())
				continue;

			int count = 0;
			if(env.target instanceof L2Playable)
				((L2Playable) env.target).setMassUpdating(true);
			for(L2Effect e : env.target.getAllEffects())
			{
				if(e == null)
					continue;

				if(e.getSkill().getAbnormalTypes().contains(slotType.toLowerCase()) && e.getSkill().getAbnormalLevel() <= maxAbnormalLevel)
				{
					env.target.stopEffect(e.getSkillId());
					count++;
				}
			}
			if(env.target instanceof L2Playable)
			{
				((L2Playable) env.target).setMassUpdating(false);
				if(count > 0)
				{
					env.target.updateEffectIcons();
					env.target.sendChanges();
				}
			}
		}
	}
}
