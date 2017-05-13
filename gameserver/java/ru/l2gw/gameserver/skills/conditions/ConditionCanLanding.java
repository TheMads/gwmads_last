package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author admin
 * @date 19.08.2010 15:29:56
 */
public class ConditionCanLanding extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(env.character.isPlayer())
		{
			L2Player player = (L2Player) env.character;
			if(player.isFlying())
			{
				int geoZ = GeoEngine.getHeight(player.getLoc(), player.getReflection());
				if(!player.isInZone(L2Zone.ZoneType.landing) || player.getZ() - geoZ > 64 || player.getZ() - geoZ < -64)
				{
					player.sendPacket(Msg.BOARDING_OR_CANCELLATION_OF_BOARDING_ON_AIRSHIPS_IS_NOT_ALLOWED_IN_THE_CURRENT_AREA);
					return false;
				}
			}
		}
		return true;
	}
}
