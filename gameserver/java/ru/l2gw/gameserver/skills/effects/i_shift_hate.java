package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.math.Rnd;

/**
 * @author: rage
 * @date: 24.09.2009 15:46:31
 */
public class i_shift_hate extends i_effect
{
	public i_shift_hate(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.success && (_template._activateRate <= 0 || Rnd.chance(_template._activateRate)))
				for(L2NpcInstance npc : cha.getKnownNpc(getSkill().getSkillRadius()))
				{
					if(npc == null || npc.isDead())
						continue;

					L2NpcInstance.AggroInfo ai = npc.getAggroList().get(cha.getObjectId());
					if(ai != null && ai.hate > 0)
					{
						npc.addDamageHate(env.target, 0, ai.hate);
						ai.hate = 0;
					}
				}
	}
}
