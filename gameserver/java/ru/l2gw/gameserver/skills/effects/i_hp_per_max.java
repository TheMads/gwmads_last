package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2SiegeHeadquarterInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;

/**
 * User: ic
 * Date: 23.04.2010
 */
public class i_hp_per_max extends i_effect
{
	private boolean excludeCaster;

	public i_hp_per_max(EffectTemplate template)
	{
		super(template);
		excludeCaster = template._attrs.getBool("excludeCaster", false);

	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if((excludeCaster && env.target == env.character) || env.target == null || env.target.isDead() || env.target instanceof L2DoorInstance || env.target instanceof L2SiegeHeadquarterInstance)
				continue;

			double newHp = calc();

			if(env.target.isStatActive(Stats.BLOCK_HP))
				newHp = 0;

			newHp = 0.01 * newHp * env.target.getMaxHp();

			int hpLimit = (int) env.target.calcStat(Stats.HP_LIMIT, env.target.getMaxHp(), null, null);

			if(calc() > 0 && env.target.getCurrentHp() + newHp > hpLimit)  // Positive effect
				newHp = Math.max(0, hpLimit - env.target.getCurrentHp());

			env.target.setCurrentHp(env.target.getCurrentHp() + newHp);

			if(calc() > 0)
			{
				if(env.target == cha && newHp >= 0)
					env.target.sendPacket(new SystemMessage(SystemMessage.S1_HP_HAVE_BEEN_RESTORED).addNumber((int) newHp));
				else if(newHp >= 0)
					env.target.sendPacket(new SystemMessage(SystemMessage.S2_HP_HAS_BEEN_RESTORED_BY_C1).addCharName(cha).addNumber((int) newHp));

				if(newHp > 0)
					cha.fireMethodInvoked(MethodCollection.onHeal, new Object[]{env.target, newHp});
			}
		}
	}
}
