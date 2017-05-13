package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ZoneManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.instances.L2EffectPointInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

/**
 * @author: rage
 * @date: 15.07.2010 13:53:55
 */
public class ConditionOpGround extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player player = (L2Player) env.character;

		if(player.getEffectPoint() != null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(env.skill.getId()));
			return false;
		}

		Location loc = player.getGroundSkillLoc();

		if(loc == null)
			loc = player.getLoc();

		if(env.skill.isOffensive())
		{
			if(player.isInZonePeace())
			{
				player.sendPacket(Msg.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_A_PEACE_ZONE);
				return false;
			}

			GArray<L2Zone> zones = ZoneManager.getInstance().getZones(loc.getX(), loc.getY(), loc.getZ());
			if(zones != null)
				for(L2Zone zone : zones)
					if(zone.isActive(player.getReflection()) && zone.getTypes().contains(L2Zone.ZoneType.peace))
					{
						player.sendPacket(Msg.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_A_PEACE_ZONE);
						return false;
					}
		}

		for(L2Character obj : L2World.getAroundCharacters(player))
		{
			if(obj instanceof L2EffectPointInstance)
			{
				L2EffectPointInstance effectPoint = (L2EffectPointInstance) obj;
				if(effectPoint.getSkill() != null && effectPoint.getSkill().isOffensive() != env.skill.isOffensive() && effectPoint.isInRange(loc, effectPoint.getSkill().getSkillRadius() + env.skill.getSkillRadius() + 140))
				{
					player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(env.skill.getId()));
					return false;
				}
			}
		}

		return true;
	}
}
