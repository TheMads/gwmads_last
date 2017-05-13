package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;

/**
 * User: ic
 * Date: 20.07.2010
 */
public class i_hp_by_level_self extends i_effect
{
	private final String silent;

	public i_hp_by_level_self(EffectTemplate template)
	{
		super(template);
		silent = template._attrs.getString("noMessage", "false");
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(cha == null || cha.isDead())
			return;

		double newHp = calc() + cha.getLevel();

		if(cha.isStatActive(Stats.BLOCK_HP))
			newHp = 0;

		int hpLimit = (int) cha.calcStat(Stats.HP_LIMIT, cha.getMaxHp(), null, null);

		if(calc() > 0 && cha.getCurrentHp() + newHp > hpLimit)  // Positive effect
			newHp = Math.max(0, hpLimit - cha.getCurrentHp());

		cha.setCurrentHp(Math.max(0, cha.getCurrentHp() + newHp));

		if(calc() > 0 && silent.equalsIgnoreCase("false") && newHp >= 0)
			cha.sendPacket(new SystemMessage(SystemMessage.S1_HP_HAVE_BEEN_RESTORED).addNumber((int) newHp));
	}
}
