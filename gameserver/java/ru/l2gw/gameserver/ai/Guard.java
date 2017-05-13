package ru.l2gw.gameserver.ai;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.instances.L2GuardInstance;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.model.instances.L2RaidBossInstance;

import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;

public class Guard extends Fighter implements Runnable
{

	public Guard(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}

	@Override
	protected boolean thinkActive()
	{
		if(_actor.isDead())
			return true;

		L2GuardInstance guard = (L2GuardInstance) _actor;

		if(getIntention() == AI_INTENTION_ACTIVE)
			for(L2Character cha : L2World.getAroundCharacters(_actor, 600, Config.PLAYER_VISIBILITY_Z))
			{
				if(Config.DEBUG)
					_log.debug(_actor + ": PK " + cha + " entered scan range");

				if(autoAttackCondition(cha))
				{
					guard.addDamageHate(cha, 0, 1);
					setIntention(AI_INTENTION_ATTACK, cha, null);
					return true;
				}
			}

		return super.thinkActive();
	}

	@Override
	public boolean checkAggression(L2Character target)
	{
		if(target.getKarma() <= 0)
			return false;
		if(_intention != CtrlIntention.AI_INTENTION_ACTIVE)
			return false;
		if(_globalAggro < 0)
			return false;
		if(!_thisActor.getAggroList().containsKey(target.getObjectId()) && !_thisActor.isInRange(target, 600))
			return false;
		if(Math.abs(target.getZ() - _actor.getZ()) > 400)
			return false;
		if(isSilent(target))
			return false;
		if(!GeoEngine.canSeeTarget(_actor, target))
			return false;
		if(target.isPlayer() && ((L2Player) target).isGM() && ((L2Player) target).isInvisible())
			return false;
		if((target.isSummon() || target.isPet()) && target.getPlayer() != null)
			_thisActor.addDamageHate(target.getPlayer(), 0, 1);

		_thisActor.addDamageHate(target, 0, 2);
		startRunningTask(2000);
		setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
		return true;
	}

	protected boolean autoAttackCondition(L2Character target)
	{
		if(target.isAlikeDead() || target.isInvul())
			return false;
		if(!GeoEngine.canSeeTarget(_actor, target))
			return false;
		if(target.getKarma() > 0)
			return true;
		if(Config.ALLOW_GUARDS && target.isMonster() && !(target instanceof L2RaidBossInstance))
			return ((L2MonsterInstance) target).isAggressive();
		return false;
	}
}
