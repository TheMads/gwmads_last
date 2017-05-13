package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 10.09.2010 19:24:15
 */
public class i_ep extends i_effect
{
	public i_ep(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(cha instanceof L2Player && ((L2Player) cha).getVehicle() instanceof L2ClanAirship)
		{
			L2ClanAirship cas = (L2ClanAirship) ((L2Player) cha).getVehicle();
			cas.setCurrentEp(cas.getCurrentEp() + (int) calc());
			cas.broadcastUserInfo();
		}
	}
}
