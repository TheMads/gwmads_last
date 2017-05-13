package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author ic
 * @date 23.12.2009
 */
public class i_dispel_by_slot_probability extends i_effect
{
	private final String slotType;
	private final double chance;

	public i_dispel_by_slot_probability(EffectTemplate template)
	{
		super(template);
		slotType = template._attrs.getString("slotType", "");
		chance = template._attrs.getDouble("chance", 5);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(slotType.equals(""))
			return;

		for(Env env : targets)
		{
			if(env.target == null || env.target.isDead() || !env.success)
				continue;

			int count = 0;
			if(env.target instanceof L2Playable)
				((L2Playable) env.target).setMassUpdating(true);

			for(L2Effect e : env.target.getAllEffects())
			{
				if(e.getSkill().getAbnormalTypes().contains(slotType.toLowerCase()))
				{
					double ch = Formulas.calcCancelChance(cha, env.target, chance, getSkill().getMagicLevel(), e);
					ch = Math.max(Math.min(Config.CANCEL_SKILLS_HIGH_CHANCE_CAP, ch), Config.CANCEL_SKILLS_LOW_CHANCE_CAP);
					if(chance >= 100 || Rnd.chance(ch))
					{
						env.target.stopEffect(e.getSkillId());
						count++;
					}
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
