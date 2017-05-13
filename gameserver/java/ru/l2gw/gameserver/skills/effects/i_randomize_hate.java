package ru.l2gw.gameserver.skills.effects;

import javolution.util.FastList;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;

import java.util.List;

/**
 * @author: rage
 * @date: 22.09.2009 13:09:52
 */
public class i_randomize_hate extends i_effect
{
	public i_randomize_hate(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(!env.target.isFearImmune() && env.target.isMonster() && env.success && (_template._activateRate <= 0 || Rnd.chance(_template._activateRate)))
			{
				L2NpcInstance npc = (L2NpcInstance) env.target;
				L2Character hated = npc.getMostHated();

				if(hated != null)
				{
					L2NpcInstance.AggroInfo hatedAI = npc.getAggroList().get(hated.getObjectId());
					List<L2Character> hateList = new FastList<L2Character>();

					for(L2NpcInstance.AggroInfo ai : npc.getAggroList().values())
						if(ai != null && ai.hate > 0 && ai.getAttacker() != null)
							hateList.add(ai.getAttacker());

					hateList.remove(hated);

					if(hateList.size() > 0)
					{
						L2Character newHated = hateList.get(Rnd.get(hateList.size()));
						L2NpcInstance.AggroInfo ai = npc.getAggroList().get(newHated.getObjectId());
						long hate = ai.hate;
						ai.hate = hatedAI.hate;
						hatedAI.hate = hate;
					}
				}
			}
	}
}
