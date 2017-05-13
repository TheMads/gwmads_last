package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2WorldRegion;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author rage
 * @date 05.08.2010 16:47:16
 */
public class t_hide extends t_effect
{
	public t_hide(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		getEffected().setHide(true);
		int avoidAggro = (int) getEffected().calcStat(Stats.AVOID_AGGRO, 0, null, null);
		for(L2Character cha : getEffected().getKnownCharacters(3000))
		{
			if(cha.getTarget() == getEffected())
				cha.setTarget(null);
			if(cha.getAI().getAttackTarget() == getEffected())
			{
				cha.abortAttack();
				cha.getAI().setAttackTarget(null);
			}
			if(cha.getCastingTarget() == getEffected())
			{
				cha.abortCast();
				cha.setCastingTarget(null);
			}
			if(cha.isNpc() && Rnd.chance(avoidAggro))
				cha.getAI().removeAttackDesire(getEffected());
		}

		if(getEffected().getCurrentRegion() != null)
			for(L2WorldRegion neighbor : getEffected().getCurrentRegion().getNeighbors())
				neighbor.removePlayerFromOtherPlayers(getEffected());
	}

	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().setHide(false);
		getEffected().broadcastUserInfo(true);
	}
}
