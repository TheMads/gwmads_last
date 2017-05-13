package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;

import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.*;

/**
 * @author: rage
 * @date: 01.08.2010 18:15:24
 */
public class ConditionCanSummonFriend extends Condition
{
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false; // currently not implemented for others

		L2Player player = (L2Player) env.character;

		if(!player.isInParty() || player.isInOlympiadMode() || player.getMountEngine().isMounted())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(env.skill.getId()));
			return false;
		}

		// Checks summoner not in arenas, siege zones, jail
		if(player.isInCombat())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_SUMMON_DURING_COMBAT));
			return false;
		}

		if(env.character.isInZoneBattle() || env.character.isInSiege() || env.character.isInZone(no_restart) || env.character.isInZone(no_escape) || env.character.isInZone(no_summon) || env.character.getX() < -166168 || env.character.getReflection() > 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
			return false;
		}

		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || player.isTransactionInProgress())
		{
			env.character.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_SUMMON_DURING_A_TRADE_OR_WHILE_USING_THE_PRIVATE_SHOPS));
			return false;
		}

		if(env.character.inObserverMode())
		{
			env.character.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
			return false;
		}

		// check target condition
		if(env.character != env.target)
		{
			if(env.target == null || !env.target.isPlayer())
			{
				player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(env.skill.getId()));
				return false;
			}

			L2Player pTarget = (L2Player) env.target;

			// Нельзя призывать торгующих персонажей
			if(pTarget.isInStoreMode() || pTarget.isTransactionInProgress())
			{
				env.character.sendPacket(new SystemMessage(SystemMessage.S1_IS_CURRENTLY_TRADING_OR_OPERATING_A_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED).addString(pTarget.getName()));
				return false;
			}

			// Нельзя призывать персонажей, которые находятся в режиме PvP
			if(pTarget.getPvpFlag() != 0 || pTarget.isRooted() || pTarget.isInCombat())
			{
				env.character.sendPacket(new SystemMessage(SystemMessage.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED).addString(pTarget.getName()));
				return false;
			}

			if(pTarget.isInOlympiadMode())
			{
				env.character.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_CURRENTLY_PARTICIPATING_IN_THE_GRAND_OLYMPIAD));
				return false;
			}

			if(pTarget.isInZoneBattle() || pTarget.isInSiege() || pTarget.isInZone(no_restart) || pTarget.isInZone(no_escape) || pTarget.isInZone(no_summon) || pTarget.getX() < -166168 || pTarget.getReflection() > 0)
			{
				env.character.sendPacket(new SystemMessage(SystemMessage.S1_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING).addString(pTarget.getName()));
				return false;
			}

			// Нельзя призывать мертвых персонажей
			if(pTarget.isAlikeDead())
			{
				env.character.sendPacket(new SystemMessage(SystemMessage.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED).addString(pTarget.getName()));
				return false;
			}

			if(pTarget.inObserverMode() || pTarget.isCombatFlagEquipped() || pTarget.isStatActive(Stats.CLAN_GATE))
			{
				env.character.sendPacket(new SystemMessage(SystemMessage.S1_IS_IN_A_STATE_WHICH_PREVENTS_SUMMONING).addString(pTarget.getName()));
				return false;
			}
		}

		return true;
	}
}
