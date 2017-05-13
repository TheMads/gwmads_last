package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 10.07.2010 14:47:34
 */
public class ConditionBuildOutpost extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player player = (L2Player) env.character;
		if(player.getClanId() == 0 || !player.isClanLeader())
			return false;

		if(player.isInZone(L2Zone.ZoneType.siege_residence) || !player.isInZone(L2Zone.ZoneType.headquarters))
		{
			player.sendPacket(Msg.YOU_CANNOT_SET_UP_A_BASE_HERE);
			return false;
		}

		if(player.getClan().getCamp() != null)
		{
			player.sendPacket(Msg.AN_OUTPOST_OR_HEADQUARTERS_CANNOT_BE_BUILT_BECAUSE_AT_LEAST_ONE_ALREADY_EXISTS);
			return false;
		}

		for(int x = -200; x <= 200; x += 200)
			for(int y = -200; y <= 200; y += 200)
				if(x != 0 && y != 0 && !GeoEngine.canMoveToCoord(player.getX(), player.getY(), player.getZ(), player.getX() + x, player.getY() + y, player.getZ(), player.getReflection()))
				{
					player.sendPacket(Msg.YOU_CANNOT_SET_UP_A_BASE_HERE);
					return false;
				}

		return TerritoryWarManager.getWar().isInProgress() && player.getTerritoryId() != 0;
	}
}

