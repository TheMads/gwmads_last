package ru.l2gw.gameserver.ai;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.L2Skill.NextAction;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.MyTargetSelected;
import ru.l2gw.gameserver.serverpackets.StopMove;
import ru.l2gw.util.Location;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

import static ru.l2gw.gameserver.ai.CtrlIntention.*;

public class L2PlayableAI extends L2CharacterAI
{
	private boolean thinking = false; // to prevent recursive thinking

	private nextAction _nextAction;
	private Object _nextAction_arg0;
	private Object _nextAction_arg1;
	private boolean _nextAction_arg2;
	private boolean _nextAction_arg3;

	protected boolean _forceUse;
	private boolean _dontMove;
	private L2ItemInstance _usedItem;

	private ScheduledFuture<?> _blockTask;
	protected ScheduledFuture<?> _followTask;
	protected ThinkFollow _thinkFollow;
	protected ScheduledFuture<?> _castTaskFuture;
	protected CastTask _castTask;
	private final ReentrantLock _followLock = new ReentrantLock();
	private final ReentrantLock togglesLock = new ReentrantLock();

	public L2PlayableAI(L2Playable actor)
	{
		super(actor);
	}

	public enum nextAction
	{
		ATTACK,
		CAST,
		MOVE,
		REST,
		PICKUP,
		INTERACT
	}

	@Override
	public void setNextAction(nextAction action, Object arg0, Object arg1, boolean arg2, boolean arg3)
	{
		_nextAction = action;
		_nextAction_arg0 = arg0;
		_nextAction_arg1 = arg1;
		_nextAction_arg2 = arg2;
		_nextAction_arg3 = arg3;
	}

	public boolean setNextIntention()
	{
		nextAction nextAction = _nextAction;
		Object nextAction_arg0 = _nextAction_arg0;
		Object nextAction_arg1 = _nextAction_arg1;
		boolean nextAction_arg2 = _nextAction_arg2;
		boolean nextAction_arg3 = _nextAction_arg3;

		if(nextAction == null)
			return false;

		if(debug)
    		_log.info(_actor + " setNextAction: " + _nextAction + " --> " + nextAction_arg0 + " forceUse: " + nextAction_arg2);

		L2Skill skill;
		L2Character target;
		L2Object object;

		switch(nextAction)
		{
			case ATTACK:
				if(!(nextAction_arg0 instanceof L2Character))
					return false;
				target = (L2Character) nextAction_arg0;
				_forceUse = nextAction_arg2;
				_dontMove = nextAction_arg3;
				clearNextAction();
				if(_intention == AI_INTENTION_CAST)
					thinking = false;
				if(debug)
    				_log.info(_actor + " setNextAction: set intention AI_INTENTION_ATTACK --> " + target);
				setIntention(AI_INTENTION_ATTACK, target);
				break;
			case CAST:
				if(!(nextAction_arg0 instanceof L2Skill) || !(nextAction_arg1 instanceof L2Character))
					return false;
				skill = (L2Skill) nextAction_arg0;
				target = (L2Character) nextAction_arg1;
				_forceUse = nextAction_arg2;
				_dontMove = nextAction_arg3;
				clearNextAction();
				if(skill.checkCondition(_actor, target, _usedItem, _forceUse, true))
					setIntention(AI_INTENTION_CAST, skill, target);
				break;
			case MOVE:
				if(!(nextAction_arg0 instanceof Location) || !(nextAction_arg1 instanceof Integer))
					return false;
				Location loc = (Location) nextAction_arg0;
				Integer offset = (Integer) nextAction_arg1;
				clearNextAction();
				_actor.moveToLocation(loc, offset, nextAction_arg2);
				break;
			case REST:
				_actor.sitDown();
				break;
			case INTERACT:
				if(nextAction_arg0 == null)
					return false;
				object = (L2Object) nextAction_arg0;
				clearNextAction();
				onIntentionInteract(object);
				break;
			case PICKUP:
				if(nextAction_arg0 == null)
					return false;
				object = (L2Object) nextAction_arg0;
				clearNextAction();
				onIntentionPickUp(object);
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public void clearNextAction()
	{
		_nextAction = null;
		_nextAction_arg0 = null;
		_nextAction_arg1 = null;
		_nextAction_arg2 = false;
		_nextAction_arg3 = false;
		cancelCastTask();
	}

	@Override
	protected void onEvtFinishCasting(L2Skill skill)
	{
		if(debug)
    		_log.info(_actor + " EvtFinishCasting");
		if(!setNextIntention())
			setIntention(AI_INTENTION_ACTIVE);
	}

	@Override
	protected void onEvtReadyToAct()
	{
		if(debug)
    		_log.info(_actor + " EvtReadyToAct");
		if(!setNextIntention())
			onEvtThink();
	}

	@Override
	protected void onEvtArrived()
	{
		if(debug)
			_log.info(_actor + " onEvtArrived: " + _intention);

		if(!setNextIntention())
			if(_intention == AI_INTENTION_INTERACT || _intention == AI_INTENTION_PICK_UP)
				onEvtThink();
			else
				changeIntention(AI_INTENTION_ACTIVE, null, null);
	}

	@Override
	protected void onEvtArrivedTarget()
	{
		if(debug)
			_log.info(_actor + " onEvtArrivedTarget: " + _intention);

		switch(_intention)
		{
			case AI_INTENTION_ATTACK:
				thinkAttack(false);
				break;
			case AI_INTENTION_CAST:
				thinkCast(false);
				break;
		}
	}

	@Override
	protected void onEvtArrivedBlocked(Location blocked_at_pos)
	{
		_actor.stopMove();
		setAttackTarget(null);
		setIntention(AI_INTENTION_ACTIVE);
	}
	
	@Override
	protected void onEvtThink()
	{
		if(debug)
			_log.info(_actor + " onEvtThink -> Check intention " + _intention + " " + thinking + " " + _actor.isActionsDisabled());

		if(thinking || _actor.isActionsDisabled())
			return;

		thinking = true;

		try
		{
			switch(_intention)
			{
				case AI_INTENTION_ATTACK:
					thinkAttack(true);
					break;
				case AI_INTENTION_CAST:
					thinkCast(true);
					break;
				case AI_INTENTION_PICK_UP:
					thinkPickUp();
					break;
				case AI_INTENTION_INTERACT:
					thinkInteract();
					break;
			}
		}
		catch(Exception e)
		{
			_log.warn("Exception onEvtThink(): " + e);
			e.printStackTrace();
		}
		finally
		{
			thinking = false;
		}
	}

	public void followToCharacter(L2Character target, int offset)
	{
		if(!_actor.followToCharacter(target, offset))
		{
			if(debug)
				_log.info(getClass().getSimpleName() + " can't follow to: " + target);
			_actor.sendActionFailed();
		}
		stopFollow();
		_followLock.lock();
		try
		{
			if(_thinkFollow == null)
				_thinkFollow = new ThinkFollow(_actor);

			if(debug)
				_log.info(getClass().getSimpleName() + " start follow task: " + target);
			_followTask = ThreadPoolManager.getInstance().scheduleAi(_thinkFollow, _actor.isPlayer() && target.isPlayer() ? 500 : 1000, _actor.isPlayer());
		}
		finally
		{
			_followLock.unlock();
		}
	}

	public static class ThinkFollow implements Runnable
	{
		private final WeakReference<L2Character> _cha;

		public ThinkFollow(L2Character cha)
		{
			_cha = new WeakReference<L2Character>(cha);
		}

		public void run()
		{
			L2Character cha = _cha.get();

			if(cha != null)
			{
				L2Character target = cha.getFollowTarget();

				if(target == null || !target.isVisible())// || cha.isPlayer() && !cha.isInRangeZ(target, 2000))
				{
					((L2PlayableAI) cha.getAI()).stopFollow();
					cha.stopMove();
					return;
				}

				boolean attack = cha.getAI().getIntention() == AI_INTENTION_ATTACK;
				boolean follow = cha.isInAirShip() && target.isInAirShip() ? cha.getPlayer().followInVehicle(target, attack ? ((L2PlayableAI) cha.getAI()).getAttackRange(true) : cha._offset) : cha.followToCharacter(target, attack ? ((L2PlayableAI) cha.getAI()).getAttackRange(true) : cha._offset);

				if(((L2PlayableAI) cha.getAI()).getDebug())
					_log.info(cha + " follow --> " + target + " " + follow);
				
				if(follow && cha instanceof L2Summon && attack && cha.getPlayer() != null && (cha.getFollowTarget() == null || !cha.getPlayer().isInRange(cha.getFollowTarget(), 3500)))
					cha.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);

				((L2PlayableAI) cha.getAI())._followTask = ThreadPoolManager.getInstance().scheduleAi(this, cha.isPlayer() && target.isPlayer() ? 500 : 1000, cha.isPlayer());
			}
		}
	}

	@Override
	protected void onIntentionInteract(L2Object object)
	{
		if(_actor.isActionsDisabled())
		{
			setNextAction(nextAction.INTERACT, object, null, false, false);
			clientActionFailed();
			return;
		}

		clearNextAction();
		changeIntention(AI_INTENTION_INTERACT, object, null);
		onEvtThink();
	}

	protected void thinkInteract()
	{
		L2Object target = (L2Object) _intention_arg0;

		if(debug)
			_log.info(_actor + " thinkInteract -> " + target);

		if(target == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}

		int range = _actor.getMinDistance(target) + 36;

		if(debug)
			_log.info(_actor + " thinkInteract -> " + target + " range: " + range + " actualRange: " + String.format("%.02f/%.02f", _actor.getDistance3D(target), _actor.getDistance(target)));

		if(_actor.isInRange(target, _actor.getInteractDistance(target)))
		{
			if(debug)
				_log.info(_actor + " thinkInteract -> " + target + " doInteract");
			if(_actor.isPlayer())
				((L2Player) _actor).doInteract(target);
			setIntention(AI_INTENTION_IDLE);
		}
		else
		{
			if(debug)
				_log.info(_actor + " thinkInteract -> " + target + " moveToLoc offset: " + (range - 12));
			_actor.moveToLocation(target.getLoc(), range - 12, true);
			setNextAction(nextAction.INTERACT, target, null, false, false);
		}
	}

	@Override
	protected void onIntentionPickUp(L2Object object)
	{
		if(_actor.isActionsDisabled())
		{
			setNextAction(nextAction.PICKUP, object, null, false, false);
			clientActionFailed();
			return;
		}

		clearNextAction();
		changeIntention(AI_INTENTION_PICK_UP, object, null);
		onEvtThink();
	}

	@Override
	protected void onIntentionFollow(L2Character target, Integer offset)
	{
		if(debug)
			_log.warn("L2PlayableAI: onIntentionFollow: isFollow: " + _actor.isFollow + " fallow Target: " + _actor.getFollowTarget());
		changeIntention(AI_INTENTION_FOLLOW, target, offset);
		followToCharacter(target, offset);
	}

	protected void thinkPickUp()
	{
		Object arg0 = _intention_arg0;
		if(!(arg0 instanceof L2Object))
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}

		L2Object target = (L2Object) arg0;
		_actor.setFollowTarget(null);
		try
		{
			if(_actor.isInRange(target, 30) && Math.abs(_actor.getZ() - target.getZ()) < 60 && (_actor.isPlayer() || _actor.isPet()))
				_actor.doPickupItem(target);
			else
			{
				_actor.moveToLocation(target.getLoc(), 10, true);
				setNextAction(nextAction.PICKUP, target, null, false, false);
			}
		}
		catch(NullPointerException e)
		{
		}
	}

	protected void thinkAttack(boolean checkRange)
	{
		if(debug)
		    _log.info(_actor + " thinkAttack: " + checkRange);
		if(_actor.isAttackingDisabled())
		{
			if(debug)
			    _log.info(_actor + " thinkAttack: attack disabled");
			_actor.sendActionFailed();
			return;
		}

		L2Character attackTarget = getAttackTarget();
		if(attackTarget == null || !attackTarget.isAttackable(getActor(), _forceUse, true))
		{
			if(debug)
			    _log.info(_actor + " thinkAttack: bad target: " + attackTarget + " force: " + _forceUse);

			stopFollow();
			setIntention(AI_INTENTION_ACTIVE);
			//if(attackTarget != null && !attackTarget.isDead())
			//	_actor.sendPacket(Msg.INVALID_TARGET);
			_actor.sendActionFailed();
			return;
		}

		int range = getAttackRange(checkRange);

		if(_actor.isFakeDeath())
			_actor.stopEffectsByName("c_fake_death");

		if(debug)
			_log.info(_actor + " thinkAttack: doAttack --> " + attackTarget + " range: " + (checkRange ? range + 12 : (int)(range * 1.2 + attackTarget.getColRadius())) + " actual range: " + _actor.getDistance(attackTarget));

		if(_actor.isInRange(attackTarget, checkRange ? range + 12 : (int)(range * 1.2 + attackTarget.getColRadius())))
		{
			stopFollow();
			clientStopMoving();
			if(debug)
			    _log.info(_actor + " thinkAttack: doAttack --> " + attackTarget);

			if(_actor.getPlayer() != null && _actor.getPlayer().isInFlyingTransform())
			{
				_actor.sendActionFailed();
				return;
			}

			_actor.doAttack(attackTarget);
		}
		else if(!_dontMove)
		{
			followToCharacter(attackTarget, range - 12);
			if(debug)
			    _log.info(_actor + " thinkAttack: doAttack --> " + attackTarget + " fallowToCharacter " + (range - 12));
		}	
		else
			_actor.sendActionFailed();
	}

	private int getAttackRange(boolean checkRange)
	{
		int range = _actor.getPhysicalAttackRange();// + (int) _actor.getColRadius();// + _actor.getMinDistance(attackTarget);
		if(range < 40)
			range = 40;

		L2Character attackTarget = getAttackTarget();
		if(checkRange && attackTarget instanceof L2DoorInstance)
			range += attackTarget.getColRadius();

		if(_actor instanceof L2Summon)
			range += (int) _actor.getColRadius();

		return range;
	}

	protected void thinkCast(boolean checkRange)
	{
		L2Character target = getAttackTarget();
		if(debug)
		    _log.info(_actor + " thinkCast: --> " + target);

		if(target == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			_actor.sendActionFailed();
			return;
		}

		int skillRange = _actor.getMagicalAttackRange(_skill);
		int range = skillRange + (int)(target.getColRadius());

		if(range < 40)
			range = 40;

		boolean canSee;
		boolean groundSkill = _skill.getSkillTargetType() == L2Skill.TargetType.ground;

		Location groundLoc = null;
		if(groundSkill)
		{
			groundLoc = _actor.getPlayer().getGroundSkillLoc();

			if(groundLoc == null)
				groundLoc = _actor.getLoc();

			canSee = GeoEngine.canSeeCoord(_actor, groundLoc.getX(), groundLoc.getY(), groundLoc.getZ() + 32, _actor.isFloating());
		}
		else
			canSee = GeoEngine.canSeeTarget(_actor, target);

		boolean noRangeSkill = _skill.getCastRange() < 0;

		if(!noRangeSkill && !canSee && (range > 200 || Math.abs(_actor.getZ() - target.getZ()) > 200))
		{
			stopFollow();
			_actor.sendPacket(Msg.CANNOT_SEE_TARGET);
			setIntention(AI_INTENTION_ACTIVE);
			_actor.sendActionFailed();
			return;
		}

		if(_actor.isFakeDeath())
			_actor.stopEffectsByName("c_fake_death");

		boolean inRange;

		if(groundSkill)
			inRange = _actor.getDistance(groundLoc.getX(), groundLoc.getY(), groundLoc.getZ()) <= range + 12;
		else
			inRange = _actor.isInRange(target, range + 12);

		if(inRange || noRangeSkill)
		{
			stopFollow();
			if(!noRangeSkill && !canSee)
			{
				_actor.sendPacket(Msg.CANNOT_SEE_TARGET);
				setIntention(AI_INTENTION_ACTIVE);
				_actor.sendActionFailed();
				return;
			}
			// Если скилл имеет следующее действие, назначим это действие после окончания действия скилла
			if(_skill.getNextAction() == NextAction.ATTACK && !_actor.equals(target))
			{
				if(debug)
    				_log.info(_actor + " thinkCast: set next ATTACK --> " + target + " firceUse: " + _forceUse);
				setNextAction(nextAction.ATTACK, target, null, _forceUse, false);
			}
			else if(_skill.getNextAction() == NextAction.SIT)
				setNextAction(nextAction.REST, null, null, false, false);
			else
				clearNextAction();

			clientStopMoving();

			if(_skill.checkCondition(_actor, target, _usedItem, _forceUse, true))
				_actor.doCast(_skill, target, _usedItem, _forceUse);
			else
			{
				if(debug)
    				_log.info(_actor + " thinkCast: set intention ACTIVE");
				if(!setNextIntention())
					setIntention(AI_INTENTION_ACTIVE);
				_actor.sendActionFailed();
			}
		}
		else if(!_dontMove)
		{
			if(groundSkill)
			{
				_actor.moveToLocation(groundLoc, _skill.getCastRange(), true);
				setNextAction(nextAction.CAST, _skill, target, _forceUse, _dontMove);
			}
			else
				followToCharacter(target, skillRange >= 400 ? range - 80 : skillRange >= 150 ? skillRange - 40 : skillRange + 2);
		}
		else
			_actor.sendPacket(Msg.YOUR_TARGET_IS_OUT_OF_RANGE);
	}

	@Override
	protected void onEvtDead(L2Character killer)
	{
		clearNextAction();
		//getActor().clearHateList();
		super.onEvtDead(killer);
	}

	@Override
	protected void onEvtFakeDeath()
	{
		clearNextAction();
		super.onEvtFakeDeath();
	}

	@Override
	protected void onEvtAggression(L2Character target, int aggro, L2Skill skill)
	{
		if(aggro > 0)
			if(_actor.getTarget() != target)
			{
				if(_actor.setTarget(target))
				{
					_actor.sendPacket(new MyTargetSelected(target.getObjectId(), 0));
					if(_actor.getCastingSkill() != null && _actor.getCastingSkill().isOffensive())
						_actor.setCastingTarget(target);
				}
			}
			else if(!_actor.isAfraid() && _blockTask == null)
			{
				_actor.block();
				Attack(target, false, false);
				_blockTask = ThreadPoolManager.getInstance().scheduleGeneral(new UnBlock(_actor), 1000);
			}
	}

	@Override
	public void Attack(L2Object target, boolean forceUse, boolean dontMove)
	{
		// Если не можем атаковать, то атаковать позже
		if(debug)
    		_log.info(_actor + " Attack --> " + target);
		if(_actor.isActionsDisabled() || _actor.isAttackingDisabled())
		{
			if(debug)
    			_log.info(_actor + " Attack --> " + target + " set next action attack");
			setNextAction(nextAction.ATTACK, target, null, forceUse, false);
			_actor.sendActionFailed();
			return;
		}

		_forceUse = forceUse;
		_dontMove = dontMove;
		if(stopFollow())
			_actor.stopMove();

		if(debug)
    		_log.info(_actor + " Attack --> " + target + " clear next, set intention");
		clearNextAction();
		setIntention(AI_INTENTION_ATTACK, target);
	}

	@Override
	public void Cast(L2Skill skill, L2Character target, L2ItemInstance usedItem, boolean forceUse, boolean dontMove)
	{
		// Если скилл альтернативного типа (например, бутылка на хп),
		// то он может использоваться во время каста других скиллов, или во время атаки, или на бегу.
		// Поэтому пропускаем дополнительные проверки.
		if(debug)
    		_log.info(_actor + " Cast --> " + target + " " + skill);
		if(skill.altUse())
		{
			if(!_actor.isPotionsDisabled() && skill.checkCondition(_actor, target, usedItem, false, true))
				_actor.altUseSkill(skill, target, usedItem);
			else
				clientActionFailed();
			return;
		}

		if(skill.isToggle())
		{
			if(_actor.isCastingNow() || _actor.isActionsBlocked())
				return;
			
			try
			{
				togglesLock.lock();
				if(_actor.getEffectBySkill(skill) != null)
					_actor.stopEffect(skill.getId());
				else if(skill.checkCondition(_actor, target, usedItem, forceUse, true))
				{
					_actor.altUseSkill(skill, _actor);
					//skill.applyEffects(_actor, _actor, false);
					if(skill.getNextAction() == NextAction.SIT)
					{
						_actor.stopMove();
						_actor.sitDown();
					}
				}
			}
			finally
			{
				togglesLock.unlock();
			}
			return;
		}

		if(_actor.isSkillDisabled(skill.getId()) && _actor.isPlayer())
		{
			if(skill.getNextAction() == NextAction.ATTACK)
			{
				if(debug)
    				_log.info(_actor + " set skill next action intention AI_INTENTION_ATTACK --> " + target);
				Attack(target, forceUse, dontMove);
				return;
			}
			else if(_actor.getPlayer().getSkillDisableTime(skill.getId()) < 500)
			{
				try
				{
					if(_castTaskFuture != null)
					{
						_castTaskFuture.cancel(true);
						_castTaskFuture = null;
					}
				}
				catch(NullPointerException e)
				{
				}
				if(_castTask == null)
					_castTask = new CastTask();

				_castTask.setCastTask(skill, target, usedItem, forceUse, dontMove);
				_castTaskFuture = ThreadPoolManager.getInstance().scheduleAi(_castTask, _actor.getPlayer().getSkillDisableTime(skill.getId()) + 10, true);
				return;
			}
		}

		// Если не можем кастовать, то использовать скилл позже
		if(_actor.isActionsDisabled())
		{
			if(!_actor.isSkillDisabled(skill.getId()) && (usedItem == null || _actor.isAttackingNow()))
				setNextAction(nextAction.CAST, skill, target, forceUse, dontMove);
			else if(_actor.isPlayer() && Config.ALT_SHOW_REUSE_MSG)
				_actor.sendSkillReuseMessage(skill.getId(), skill.getDisplayLevel(), usedItem);
			return;
		}

		//_actor.stopMove(null);
		_forceUse = forceUse;
		_dontMove = dontMove;
		_usedItem = usedItem;
		if(stopFollow())
			_actor.stopMove();

		if(debug)
    		_log.info(_actor + " Cast --> " + target + " clear next, set intention.");
		clearNextAction();
		setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
	}

	public class UnBlock implements Runnable
	{
		private L2Character _player;

		public UnBlock(L2Character cha)
		{
			_player = cha;
		}

		public void run()
		{
			_player.unblock();
			_blockTask = null;
		}
	}

	@Override
	public L2Playable getActor()
	{
		return (L2Playable) super.getActor();
	}

	private void cancelCastTask()
	{
		try
		{
			if(_castTaskFuture != null)
				_castTaskFuture.cancel(false);
			_castTaskFuture = null;
		}
		catch(NullPointerException e)
		{
		}
	}

	public boolean stopFollow()
	{
		//_log.info(_actor + " stop follow task");
		if(!_actor.isMoving && _actor.isPawn)
			_actor.broadcastPacket(new StopMove(_actor));

		_actor.isPawn = false;

		_followLock.lock();
		try
		{
			if(_followTask != null)
			{
				_followTask.cancel(true);
				_followTask = null;
				return true;
			}
		}
		finally
		{
			_followLock.unlock();
		}

		return false;
	}

	private class CastTask implements Runnable
	{
		private L2Skill _skill;
		private L2Character _target;
		private L2ItemInstance _usedItem;
		private boolean _forceUse, _dontMove;

		public void run()
		{
			Cast(_skill, _target, _usedItem, _forceUse, _dontMove);
		}

		public void setCastTask(L2Skill skill, L2Character target, L2ItemInstance usedItem, boolean forceUse, boolean dontMove)
		{
			_skill = skill;
			_target = target;
			_usedItem = usedItem;
			_forceUse = forceUse;
			_dontMove = dontMove;
		}
	}
	
	public void setDebug(boolean d)
	{
		debug = d;
	}
	
	public boolean getDebug()
	{
		return debug;
	}
}