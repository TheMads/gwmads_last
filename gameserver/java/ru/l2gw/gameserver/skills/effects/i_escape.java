package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 01.08.2010 15:35:56
 */
public class i_escape extends i_effect
{
	private final int _type;
	public i_escape(EffectTemplate template)
	{
		super(template);
		String type = template._attrs.getString("type", "town");
		if(type.equalsIgnoreCase("agit"))
			_type = 1;
		else if(type.equalsIgnoreCase("castle"))
			_type = 2;
		else if(type.equalsIgnoreCase("fortress"))
			_type = 3;
		else
			_type = 0;
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(!env.target.isPlayer())
				continue;

			L2Player pcTarget = env.target.getPlayer();

			if(pcTarget.isCombatFlagEquipped() || pcTarget.isInOlympiadMode() || pcTarget.isInDuel())
				continue;

			if(pcTarget.isInBoat() && pcTarget.getStablePoint() != null && !pcTarget.getVehicle().isAirShip())
			{
				pcTarget.teleToLocation(pcTarget.getStablePoint());
				continue;
			}

			env.target.abortAttack();
			env.target.abortCast();
			env.target.sendActionFailed();
			//int reflection = env.target.isInZone(ZoneType.instance) ? 0 : env.target.getReflection();

			switch(_type)
			{
				case 1:
					pcTarget.teleToClanhall();
					continue;
				case 2:
					pcTarget.teleToCastle();
					continue;
				case 3:
					pcTarget.teleToFortress();
					continue;
				default:
					boolean tele = true;
					GArray<L2Zone> zones = env.target.getZones();
					if(zones != null)
						for(L2Zone zone : zones)
							if(zone != null && zone.getRestartPoints() != null && !zone.getTypes().contains(ZoneType.residence) && !zone.getTypes().contains(ZoneType.siege) && !zone.getTypes().contains(ZoneType.siege_residence) && !zone.getTypes().contains(ZoneType.olympiad_stadia))
							{
								env.target.teleToLocation(zone.getSpawn(env.target), 0);
								tele = false;
								break;
							}
					if(tele)
						env.target.teleToClosestTown();
					break;
			}
		}
	}
}
