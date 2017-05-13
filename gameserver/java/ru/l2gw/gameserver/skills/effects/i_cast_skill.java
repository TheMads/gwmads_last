package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.SkillTable;

public class i_cast_skill extends i_effect
{
	private final int skillId;
	private final int skillLevel;

	public i_cast_skill(EffectTemplate template)
	{
		super(template);
		skillId = template._attrs.getInteger("skillId", 0);
		skillLevel = template._attrs.getInteger("skillLvl", 0);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);

		if(skill == null)
			return;

		for(Env env : targets)
		{
			try
			{
				boolean calcChance = true;
				if(cha == env.target)
				{
					env.target = cha;
					calcChance = false;
				}

				if(skill.getIncreaseLevel() > 0 && env.target.getEffectBySkillId(skillId) != null)
				{
					L2Effect ef = env.target.getEffectBySkillId(skillId);
					if(ef != null && ef.getSkillLevel() <= skill.getIncreaseLevel())
						skill = SkillTable.getInstance().getInfo(skillId, ef.getSkillLevel() + 1);
				}

				if(skill != null)
				{
					Env e = new Env(cha, env.target, skill);
					e.value = skill.getActivateRate();
					skill.applyEffects(e, calcChance, 1);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
