package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2SiegeHeadquarterInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 10.07.2010 13:16:16
 */
public class ConditionBuildCamp extends Condition
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

		for(L2NpcInstance npc : player.getKnownNpc(150))
			if(npc instanceof L2SiegeHeadquarterInstance)
			{
				player.sendPacket(Msg.YOU_MAY_NOT_BUILD_YOUR_HEADQUARTERS_IN_CLOSE_PROXIMITY_TO_ANOTHER_HEADQUARTERS);
				return false;
			}

		if(player.getClan().getCamp() != null)
		{
			player.sendPacket(Msg.AN_OUTPOST_OR_HEADQUARTERS_CANNOT_BE_BUILT_BECAUSE_AT_LEAST_ONE_ALREADY_EXISTS);
			return false;
		}

		for(int x = -150; x <= 150; x += 150)
			for(int y = -150; y <= 150; y += 150)
				if(x != 0 && y != 0 && !GeoEngine.canMoveToCoord(player.getX(), player.getY(), player.getZ(), player.getX() + x, player.getY() + y, player.getZ(), player.getReflection()))
				{
					player.sendPacket(Msg.YOU_CANNOT_SET_UP_A_BASE_HERE);
					return false;
				}

		if(TerritoryWarManager.getWar().isInProgress())
			return player.getTerritoryId() != 0;

		Siege siege = SiegeManager.getSiege(player);
		return !(siege == null || !siege.isInProgress() || siege.getAttackerClan(player.getClanId()) == null || player.getClanId() == 0 || !player.isClanLeader());
	}
}
