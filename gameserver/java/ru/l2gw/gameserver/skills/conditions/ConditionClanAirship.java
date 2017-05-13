package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 10.09.2010 19:22:04
 */
public class ConditionClanAirship extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		return env.character instanceof L2Player && ((L2Player) env.character).getVehicle() instanceof L2ClanAirship;
	}
}
