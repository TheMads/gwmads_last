package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;

/**
 * User: ic
 * Date: 22.04.2010
 */
public class i_mp_per_max extends i_effect
{
	private boolean excludeCaster;

	public i_mp_per_max(EffectTemplate template)
	{
		super(template);
		excludeCaster = template._attrs.getBool("excludeCaster", false);

	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if((excludeCaster && env.target == env.character) || env.target == null || env.target.isDead())
				continue;

			double newMp = calc();

			if(env.target.isStatActive(Stats.BLOCK_MP))
				newMp = 0;

			newMp = 0.01 * newMp * env.target.getMaxMp();

			int mpLimit = (int) env.target.calcStat(Stats.MP_LIMIT, env.target.getMaxMp(), null, null);

			if(newMp > 0)  // Positive effect
			{
				if(cha != env.target)
				{
					newMp = env.target.calcStat(Stats.MANAHEAL_EFFECTIVNESS, newMp, null, null);
					newMp += env.target.calcStat(Stats.MANAHEAL_EFFECTIVNESS_STATIC, 0, null, null);
				}

				if(env.target.getCurrentMp() + newMp > mpLimit)
					newMp = Math.max(0, mpLimit - env.target.getCurrentMp());
			}

			int oldMp = (int) env.target.getCurrentMp();
			env.target.setCurrentMp(env.target.getCurrentMp() + newMp);
			newMp = env.target.getCurrentMp() - oldMp;

			if(newMp >= 0)
			{
				if(env.target == cha)
					env.target.sendPacket(new SystemMessage(SystemMessage.S1_MP_HAVE_BEEN_RESTORED).addNumber((int) newMp));
				else
					env.target.sendPacket(new SystemMessage(SystemMessage.S2_MP_HAS_BEEN_RESTORED_BY_C1).addCharName(cha).addNumber((int) newMp));
			}
		}
	}
}
