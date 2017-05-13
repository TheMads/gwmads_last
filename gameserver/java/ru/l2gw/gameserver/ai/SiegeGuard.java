package ru.l2gw.gameserver.ai;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2SiegeGuardInstance;
import ru.l2gw.gameserver.model.instances.L2TerritoryGuardInstance;
import ru.l2gw.gameserver.skills.Stats;

public abstract class SiegeGuard extends DefaultAI
{
	protected L2SiegeGuardInstance _thisActor;
	protected int _territoryId;

	public SiegeGuard(L2Character actor)
	{
		super(actor);
		_thisActor = (L2SiegeGuardInstance) actor;
		MAX_PATHFIND_FAILS = Integer.MAX_VALUE;
		MAX_ATTACK_TIMEOUT = Integer.MAX_VALUE;
		AI_TASK_DELAY = 1000;
		MAX_PURSUE_RANGE = 2500;
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}

	@Override
	protected boolean randomAnimation()
	{
		return false;
	}

	@Override
	protected boolean isSilent(L2Character target)
	{
		// Осадные гварды могут видеть игроков в режиме Silent Move с вероятностью 1%
		return Rnd.chance((int) target.calcStat(Stats.AVOID_AGGRO, 0, null, null)) && Rnd.chance(90);
	}

	@Override
	public boolean checkAggression(L2Character target)
	{
		if(!(target instanceof L2Playable))
			return false;
		if(_thisActor instanceof L2TerritoryGuardInstance)
			return target.getTerritoryId() > 0 && target.getTerritoryId() != _thisActor.getTerritoryId() && super.checkAggression(target);
		else if(target.getPlayer() != null && _thisActor.getSiege().checkIsDefender(target.getClanId()))
			return false;
		return super.checkAggression(target);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage, L2Skill skill)
	{
		if(attacker != null && attacker.getPlayer() != null)
		{
			if(_thisActor instanceof L2TerritoryGuardInstance)
			{
				if(_thisActor.getTerritoryId() == attacker.getTerritoryId())
					return;
			}
		}
		super.onEvtAttacked(attacker, damage, skill);
	}

	@Override
	protected void onEvtAggression(L2Character target, int aggro, L2Skill skill)
	{
		if(target != null && target.getPlayer() != null)
		{
			if(_thisActor instanceof L2TerritoryGuardInstance)
			{
				if(_thisActor.getTerritoryId() == target.getTerritoryId())
					return;
			}
		}
		super.onEvtAggression(target, aggro, skill);
	}

	@Override
	protected boolean thinkActive()
	{
		return super.thinkActive() && !randomAnimation() && !randomWalk();
	}

	@Override
	protected void thinkAttack()
	{
		_thisActor.setRunning();
		_thisActor.setAttackTimeout(Integer.MAX_VALUE);

		super.thinkAttack();
	}
}