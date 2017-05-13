package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.arrays.GArray;

/**
 * User: ic
 * Date: 22.04.2010
 */
public class i_mp_by_level extends i_effect
{
	public i_mp_by_level(EffectTemplate template)
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

			double newMp = getSkill().getPower(cha, env.target);

			newMp -= 1;
			newMp = env.target.calcStat(Stats.MANAHEAL_EFFECTIVNESS, newMp, null, null);
			newMp += env.target.calcStat(Stats.MANAHEAL_EFFECTIVNESS_STATIC, 0, null, null);

			if(env.target.getLevel() - cha.getLevel() > 3)
			{
				newMp = newMp - 0.167 * (env.target.getLevel() - cha.getLevel() - 3) * newMp;
				if(newMp < 0)
					newMp = 0;
			}

			if(env.target.isStatActive(Stats.BLOCK_MP))
				newMp = 0;

			int mpLimit = (int) env.target.calcStat(Stats.MP_LIMIT, env.target.getMaxMp(), null, null);

			if(newMp > 0 && env.target.getCurrentMp() + newMp > mpLimit)
				newMp = Math.max(0, mpLimit - env.target.getCurrentMp());

			int oldMp = (int) env.target.getCurrentMp();
			env.target.setCurrentMp(env.target.getCurrentMp() + newMp);
			newMp = env.target.getCurrentMp() - oldMp;

			if(getSkill().getPower(cha, env.target) > 0)
			{
				if(env.target == cha)
					env.target.sendPacket(new SystemMessage(SystemMessage.S1_MP_HAVE_BEEN_RESTORED).addNumber((int) newMp));
				else
					env.target.sendPacket(new SystemMessage(SystemMessage.S2_MP_HAS_BEEN_RESTORED_BY_C1).addCharName(cha).addNumber((int) newMp));
			}
		}
	}
}
