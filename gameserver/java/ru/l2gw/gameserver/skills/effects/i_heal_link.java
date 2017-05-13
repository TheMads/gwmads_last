package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 16.01.12 15:01
 */
public class i_heal_link extends i_effect
{
	private final double maxPer, decPer;

	public i_heal_link(EffectTemplate template)
	{
		super(template);
		String[] options = _template._options.split(";");
		maxPer = options.length > 0 ? Double.parseDouble(options[0]) * 0.01 : 0.30;
		decPer = options.length > 1 ? Double.parseDouble(options[1]) * 0.01 : 0.03;
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		double hpPer = maxPer;
		for(Env env : targets)
		{
			if(env.target == null || env.target.isDead())
				continue;

			double newHp = env.target.getMaxHp() * hpPer;

			if(env.target.isStatActive(Stats.BLOCK_HP))
				newHp = 0;

			// Player holding a cursed weapon can't be healed and can't heal
			if(env.target != cha)
				if(env.target.isPlayer() && env.target.isCursedWeaponEquipped())
					newHp = 0;
				else if(cha.isPlayer() && cha.isCursedWeaponEquipped())
					newHp = 0;

			int hpLimit = (int) env.target.calcStat(Stats.HP_LIMIT, env.target.getMaxHp(), null, null);

			if(env.target.getCurrentHp() + newHp > hpLimit)
				newHp = hpLimit - env.target.getCurrentHp();

			if(newHp < 0)
				newHp = 0;

			env.target.setCurrentHp(newHp + env.target.getCurrentHp());
			if(env.target == cha && newHp >= 0)
				env.target.sendPacket(new SystemMessage(SystemMessage.S1_HP_HAVE_BEEN_RESTORED).addNumber((int) newHp));
			else if(newHp >= 0)
				env.target.sendPacket(new SystemMessage(SystemMessage.S2_HP_HAS_BEEN_RESTORED_BY_C1).addCharName(cha).addNumber((int) newHp));

			hpPer -= decPer;

			if(newHp > 0)
				cha.fireMethodInvoked(MethodCollection.onHeal, new Object[]{env.target, newHp});
		}
	}
}
