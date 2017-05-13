package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.math.Rnd;

/**
 * @author: rage
 * @date: 18.07.2010 12:03:28
 */
public class c_fake_death extends t_effect
{
	public c_fake_death(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}
/*
	@Override
	public EffectType getEffectType()
	{
		return EffectType.continuous;
	}
*/
	@Override
	public void onStart()
	{
		super.onStart();
		startActionTask(3000);
		getEffected().startFakeDeath();
		int avoidAggro = (int) getEffected().calcStat(Stats.AVOID_AGGRO, 0, null, null);
		for(L2NpcInstance npc : getEffected().getKnownNpc(3000))
			if(Rnd.chance(avoidAggro))
				npc.getAI().removeAttackDesire(getEffected());
	}

	@Override
	public void onExit()
	{
		super.onExit();
		// 5 секунд после FakeDeath на персонажа не агрятся мобы
		getEffected().setNonAggroTime(System.currentTimeMillis() + 5000);
		getEffected().stopFakeDeath();
	}

	@Override
	public boolean onActionTime()
	{
		if(getEffected().isDead())
			return false;

		double mp = getEffected().getCurrentMp() + calcTickVal();
		if(mp < 0)
		{
			getEffected().sendPacket(Msg.NOT_ENOUGH_MP);
			getEffected().setCurrentMp(0);
			return false;
		}
		getEffected().setCurrentMp(mp);
		return true;
	}
}
