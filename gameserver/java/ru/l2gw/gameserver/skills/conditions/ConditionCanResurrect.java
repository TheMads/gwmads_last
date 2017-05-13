package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author admin
 * @date 03.08.2010 13:41:48
 */
public class ConditionCanResurrect extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player player = (L2Player) env.character;
		if(player.isInOlympiadMode() || player.inObserverMode() || player.isActionBlocked(L2Zone.BLOCKED_SKILL_RESURRECT))
			return false;

		if(env.target instanceof L2PetInstance && env.target.isDead() && env.target.getPlayer() == env.character)
			return true;

		SystemMessage sm = checkSiegeCond(env.character, player);
		if(sm != null)
		{
			player.sendPacket(sm);
			return false;
		}

		if(env.character != env.target)
		{
			if(!(env.target instanceof L2Playable))
				return false;

			L2Playable target = (L2Playable) env.target;

			if(target.getPlayer() == null || target.getPlayer().isInOlympiadMode() || target.getPlayer().inObserverMode() || target.isActionBlocked(L2Zone.BLOCKED_SKILL_RESURRECT))
				return false;

			sm = checkSiegeCond(env.character, target);
			if(sm != null)
			{
				player.sendPacket(sm);
				return false;
			}

			if(!target.isDead())
			{
				player.sendPacket(Msg.INVALID_TARGET);
				return false;
			}

			if(target.isPet())
			{
				if(target.getPlayer() == null)
				{
					player.sendPacket(Msg.INVALID_TARGET);
					return false;
				}
				if(target.getPlayer().isReviveRequested())
				{
					if(target.getPlayer().isRevivingPet())
						player.sendPacket(Msg.BETTER_RESURRECTION_HAS_BEEN_ALREADY_PROPOSED);
					else
						player.sendPacket(Msg.SINCE_THE_MASTER_WAS_IN_THE_PROCESS_OF_BEING_RESURRECTED_THE_ATTEMPT_TO_RESURRECT_THE_PET_HAS_BEEN_CANCELLED);
					return false;
				}
			}
			else if(target.isPlayer())
			{
				if(((L2Player) target).isReviveRequested())
				{
					if(((L2Player) target).isRevivingPet())
						player.sendPacket(Msg.SINCE_THE_PET_WAS_IN_THE_PROCESS_OF_BEING_RESURRECTED_THE_ATTEMPT_TO_RESURRECT_ITS_MASTER_HAS_BEEN_CANCELLED); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
					else
						player.sendPacket(Msg.BETTER_RESURRECTION_HAS_BEEN_ALREADY_PROPOSED); // Resurrection is already been proposed.
					return false;
				}
			}
		}

		return true;
	}

	public static SystemMessage checkSiegeCond(L2Character caster, L2Playable playable)
	{
		if(playable == null)
			return Msg.INVALID_TARGET;

		if(playable instanceof L2PetInstance && playable.getPlayer() == caster)
			return null;

		if(playable.isInSiege())
		{
			Siege siege = SiegeManager.getSiege(playable);
			if(siege != null && siege.isInProgress())
			{
				if(siege.checkIsAttacker(playable.getClanId()))
				{
					if(playable.getPlayer().getClan().getCamp() == null)
						return Msg.IF_A_BASE_CAMP_DOES_NOT_EXIST_RESURRECTION_IS_NOT_POSSIBLE;
				}
				else if(siege.checkIsDefender(playable.getClanId()))
				{
					if(siege.getSiegeUnit().isCastle && siege.getKilledCtCount() > 0)
						return Msg.THE_GUARDIAN_TOWER_HAS_BEEN_DESTROYED_AND_RESURRECTION_IS_NOT_POSSIBLE;
					else if(!siege.getSiegeUnit().isCastle)
						return Msg.IT_IS_IMPOSSIBLE_TO_BE_RESSURECTED_IN_BATTLEFIELDS_WHERE_SIEGE_WARS_ARE_IN_PROCESS;
				}
				else
					return Msg.IT_IS_IMPOSSIBLE_TO_BE_RESSURECTED_IN_BATTLEFIELDS_WHERE_SIEGE_WARS_ARE_IN_PROCESS;
			}
			else if(TerritoryWarManager.getWar().isInProgress() && playable.getTerritoryId() > 0)
			{
				L2NpcInstance camp = ResidenceManager.getInstance().getCastleById(playable.getTerritoryId() - 80).getOwner().getCamp();
				if(camp == null)
					return Msg.IF_A_BASE_CAMP_DOES_NOT_EXIST_RESURRECTION_IS_NOT_POSSIBLE;
			}
			else
				return Msg.IT_IS_IMPOSSIBLE_TO_BE_RESSURECTED_IN_BATTLEFIELDS_WHERE_SIEGE_WARS_ARE_IN_PROCESS;
		}

		return null;
	}
}
