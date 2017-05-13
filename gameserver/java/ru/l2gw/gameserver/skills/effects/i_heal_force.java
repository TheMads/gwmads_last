package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 26.10.2009 11:01:02
 */
public class i_heal_force extends i_effect
{
	public i_heal_force(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			double newHp = calc() * env.target.getMaxHp() / 100;

			if(env.target.getCurrentHp() + newHp > env.target.getMaxHp())
				newHp = env.target.getMaxHp() - env.target.getCurrentHp();

			if(newHp < 0)
				newHp = 0;

			if(env.target == cha)
				env.target.sendPacket(new SystemMessage(SystemMessage.S1_HP_HAVE_BEEN_RESTORED).addNumber((int) newHp));
			else
				env.target.sendPacket(new SystemMessage(SystemMessage.S2_HP_HAS_BEEN_RESTORED_BY_C1).addCharName(cha).addNumber((int) newHp));
			env.target.setCurrentHp(newHp + env.target.getCurrentHp());

			if(newHp > 0)
				cha.fireMethodInvoked(MethodCollection.onHeal, new Object[]{env.target, newHp});
		}
	}
}
