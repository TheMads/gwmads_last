package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.arrays.GArray;

import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.no_escape;
import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.no_restart;
import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.no_summon;

/**
 * @author: rage
 * @date: 01.08.2010 18:21:13
 */
public class i_summon_friend extends i_effect
{
	private final boolean request;
	public i_summon_friend(EffectTemplate template)
	{
		super(template);
		request = template._attrs.getBool("request", false);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(!env.target.isPlayer() || env.target == cha)
				continue;

			L2Player target = (L2Player) env.target;

			// CHECK TARGET CONDITIONS
			if(request)
			{
				if(target.isTeleportRequested())
					cha.sendPacket(new SystemMessage(SystemMessage.S1_HAS_ALREADY_BEEN_SUMMONED).addCharName(target));
				else
					target.teleportRequest(cha.getPlayer(), getSkill().getTargetConsume(), getSkill().getTargetConsumeId());
			}
			else
			{
				SystemMessage sm = checkSummonCond(target);

				if(sm != null)
				{
					cha.sendPacket(sm);
					continue;
				}

				if(getSkill().getTargetConsume() != 0 && (target.getInventory().getItemByItemId(getSkill().getTargetConsumeId()) == null || target.getInventory().getItemByItemId(getSkill().getTargetConsumeId()).getCount() < getSkill().getTargetConsume()))
				{
					target.sendPacket(new SystemMessage(SystemMessage.S1_IS_REQUIRED_FOR_SUMMONING).addItemName(getSkill().getTargetConsumeId()));
					continue;
				}

				if(getSkill().getTargetConsume() != 0)
					target.destroyItemByItemId("Consume", getSkill().getTargetConsumeId(), getSkill().getTargetConsume(), cha, true);

				target.abortCast();
				target.teleToLocation(cha.getX(), cha.getY(), cha.getZ());
			}
		}
	}
	
	public static SystemMessage checkSummonCond(L2Player target)
	{
		if(target.isAlikeDead())
			return new SystemMessage(SystemMessage.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED).addString(target.getName());

		if(target.isInStoreMode())
			return new SystemMessage(SystemMessage.S1_IS_CURRENTLY_TRADING_OR_OPERATING_A_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED).addString(target.getName());

		// Target cannot be in combat (or dead, but that's checked by TARGET_PARTY)
		if(target.isRooted() || target.isInCombat())
			return new SystemMessage(SystemMessage.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED).addString(target.getName());

		// Check for the the target's festival status
		if(target.isInOlympiadMode())
			return new SystemMessage(SystemMessage.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_CURRENTLY_PARTICIPATING_IN_THE_GRAND_OLYMPIAD);

		// Check for the target's jail status, arenas and siege zones
		if(target.isInZoneBattle() || target.isInSiege() || target.isInZone(no_restart) || target.isInZone(no_escape) || target.isInZone(no_summon) || target.getX() < -166168 || target.getReflection() > 0)
			return new SystemMessage(SystemMessage.S1_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING).addString(target.getName());

		if(target.inObserverMode() || target.isCombatFlagEquipped() || target.isStatActive(Stats.CLAN_GATE))
			return new SystemMessage(SystemMessage.S1_IS_IN_A_STATE_WHICH_PREVENTS_SUMMONING).addCharName(target);

		return null;
	}
}
