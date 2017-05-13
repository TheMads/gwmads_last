package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.model.instances.L2SiegeGuardInstance;
import ru.l2gw.gameserver.model.instances.L2SiegeHeadquarterInstance;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

final class t_fear extends t_effect
{
	public static final int FEAR_RANGE = 900;

	public t_fear(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		if(getEffected().isFearImmune() || getEffected().isAfraid())
		{
			_effect.exit();
			return;
		}

		if((getEffected().isSummon() || getEffected().isPet()) && getEffected().getPlayer() == getEffector())
		{
			_effect.exit();
			return;
		}
		super.onStart();
		if(getEffected() instanceof L2Summon)
			getEffected().setFollowStatus(false);
		
		getEffected().startFear();

		//startActionTask(_template._ticks * 1000);
		//onActionTime();

		// Fear нельзя наложить на осадных саммонов
		if(getEffected().getNpcId() == L2Summon.SIEGE_GOLEM_ID || getEffected().getNpcId() == L2Summon.SIEGE_CANNON_ID || getEffected().getNpcId() == L2Summon.SWOOP_CANNON_ID)
			return;

		if(getEffected() instanceof L2SiegeGuardInstance)
			return;
		// Fear skills cannot be used on Headquarters Flag.
		if(getEffected() instanceof L2SiegeHeadquarterInstance)
			return;

		Location pos = Util.correctCollision(getEffector().getX(),getEffector().getY(), getEffected().getLoc(), FEAR_RANGE);
		Location loc = GeoEngine.moveCheck(getEffected().getX(), getEffected().getY(), getEffected().getZ(), pos.getX(), pos.getY(), getEffected().getReflection());

		getEffected().setRunning();
		getEffected().moveToLocation(loc, 0, false);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().stopFear();
		if(getEffected() instanceof L2Summon)
			getEffected().setFollowStatus(true);
	}
}