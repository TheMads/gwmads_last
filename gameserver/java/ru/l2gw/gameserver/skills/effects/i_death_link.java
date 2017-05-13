package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 31.12.2009 13:35:34
 */
public class i_death_link extends i_effect
{
	public i_death_link(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target.isDead())
				continue;

			boolean shield = Formulas.calcShldUse(cha, env.target);
			double damage = Formulas.calcMagicDam(cha, env.target, getSkill(), shield, ss) * 1.8 * (1. - cha.getCurrentHp() / cha.getMaxHp());

			boolean blockHp = env.target.isStatActive(Stats.BLOCK_HP);
			if(blockHp)
				damage = 0;

			if(getSkill().getPower(cha, env.target) > 0)
			{
				if(damage > 0 && damage < 2)
					damage = Math.ceil(damage);
				if(damage == 0 && !blockHp)
					env.target.sendPacket(new SystemMessage(SystemMessage.S1_HAS_RECEIVED_DAMAGE_OF_S3_FROM_S2).addCharName(env.target).addCharName(cha).addNumber(0));

				cha.sendDamageMessage(env.target, (int) damage, false, false, blockHp);
				env.target.reduceHp(damage, cha, false, false);
			}
		}
	}
}
