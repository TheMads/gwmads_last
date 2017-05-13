package ru.l2gw.gameserver.ai;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.concurrent.ScheduledFuture;

import static ru.l2gw.gameserver.ai.CtrlIntention.*;

public class L2SummonAI extends L2PlayableAI
{
	private Stack<IntentionCommand> _interuptedIntentions = new Stack<IntentionCommand>();
	private RunOnAttacked _runOnAttacked;
	private ScheduledFuture<?> _runOnAttackedTask;

	class IntentionCommand
	{
		protected CtrlIntention _crtlIntention;
		protected Object _arg0, _arg1;

		protected IntentionCommand(CtrlIntention pIntention, Object pArg0, Object pArg1)
		{
			_crtlIntention = pIntention;
			_arg0 = pArg0;
			_arg1 = pArg1;
		}
	}

	public L2SummonAI(L2Summon actor)
	{
		super(actor);
	}

	@Override
	protected void onIntentionIdle()
	{
		if(debug)
			_log.warn("L2SummonAI: onIntentionIdle -> " + getIntention());

		CtrlIntention oldInt = _intention;

		getActor().stopMove();
		getActor().abortAttack();
		getActor().abortCast();
		changeIntention(AI_INTENTION_IDLE, null, null);
		clearNextAction();


		if(oldInt != AI_INTENTION_CAST && _interuptedIntentions != null && !_interuptedIntentions.isEmpty())
		{
			IntentionCommand cmd = null;
			try
			{
				cmd = _interuptedIntentions.pop();
			}
			catch(EmptyStackException ese)
			{
			}

			if(debug)
				_log.warn("L2SummonAI: onIntentionIdle restore intention -> " + cmd._crtlIntention + " " + cmd._arg0 + " " + cmd._arg1);

			if(cmd != null) // previous state shouldn't be casting
			{
				if(cmd._crtlIntention == AI_INTENTION_ATTACK)
					_forceUse = true;
				setIntention(cmd._crtlIntention, cmd._arg0, cmd._arg1);
			}
		}
	}

	@Override
	protected void onIntentionActive()
	{
		if(!_actor.isVisible())
			return;

		if(getActor().isPosessed())
		{
			getActor().setRunning();
			if(getIntention() != AI_INTENTION_ATTACK)
				setIntention(CtrlIntention.AI_INTENTION_ATTACK, getActor().getPlayer(), null);
			return;
		}

		if(getActor().getFollowStatus())
			setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getActor().getPlayer(), 55);
		else
			setIntention(AI_INTENTION_IDLE);
	}

	@Override
	protected void thinkAttack(boolean checkRange)
	{
		if(getActor().isPosessed())
			setAttackTarget(getActor().getPlayer());

		_actor.setFollowTarget(null);

		L2Character target = getAttackTarget();

		if(debug)
			_log.warn("L2SummonAI: thinkAttack -> " + target + " checkRange: " + checkRange);

		if(target == null || target.isDead())
		{
			setAttackTarget(null);
			setIntention(AI_INTENTION_IDLE);
			if(debug)
				_log.warn("L2SummonAI: thinkAttack -> " + _actor.getTarget() + " Target lost");
		}
		else
			super.thinkAttack(checkRange);
	}

	@Override
	protected void thinkCast(boolean checkRange)
	{
		if(getAttackTarget() == null)
		{
			setIntention(AI_INTENTION_IDLE);
			_actor.sendActionFailed();
			return;
		}

		_actor.setFollowTarget(null);
		super.thinkCast(checkRange);
	}

	@Override
	protected void onEvtThink()
	{
		if(getActor().isPosessed())
		{
			setAttackTarget(getActor().getPlayer());
			_intention = AI_INTENTION_ATTACK;
		}

		if(debug)
			_log.warn("L2SummonAI: onEvtThink " + _actor.getTarget());

		super.onEvtThink();
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage, L2Skill skill)
	{
		if(debug)
			_log.warn("L2SummonAI: onEvtAttacked -> " + getIntention() + " attacker: " + attacker);

		if(_runOnAttacked != null)
			_runOnAttacked.setAttacker(attacker);

		if(_runOnAttacked == null && (_intention == AI_INTENTION_FOLLOW || _intention == AI_INTENTION_IDLE || _intention == AI_INTENTION_ACTIVE) && !_actor.isMoving && attacker != _actor.getPlayer())
		{
			if(_runOnAttacked == null)
			{
				if(debug)
					_log.warn("L2SummonAI: onEvtAttacked -> " + getIntention() + " create runOnAttack");
				_runOnAttacked = new RunOnAttacked();
				_runOnAttacked.setAttacker(attacker);
			}

			if(_runOnAttackedTask == null)
			{
				if(debug)
					_log.warn("L2SummonAI: onEvtAttacked -> " + getIntention() + " run runOnAttack");
				_runOnAttackedTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(_runOnAttacked, 0, 500);
			}
		}
		super.onEvtAttacked(attacker, damage, skill);
	}

	@Override
	protected void onEvtFinishCasting(L2Skill skill)
	{
		if(debug)
			_log.warn("L2SummonAI: onEvtFinishCasting -> " + getIntention());

		if(!setNextIntention() && !_interuptedIntentions.isEmpty())
		{
			IntentionCommand cmd = null;
			try
			{
				cmd = _interuptedIntentions.pop();
			}
			catch(EmptyStackException ese)
			{
			}

			if(debug)
				_log.warn("L2SummonAI: onEvtFinishCasting restore intention " + cmd._crtlIntention + " " + cmd._arg0 + " " + cmd._arg1);

			if(cmd != null && cmd._crtlIntention != AI_INTENTION_CAST) // previous state shouldn't be casting
			{
				if(cmd._crtlIntention == AI_INTENTION_ATTACK)
					_forceUse = true;
				setIntention(cmd._crtlIntention, cmd._arg0, cmd._arg1);
			}

			//return;
		}
		else if(debug)
			_log.warn("L2SummonAI: no previous intention set... Setting it to IDLE");

		//super.onEvtFinishCasting(skill);
	}

	@Override
	protected void onEvtArrived()
	{
		if(debug)
			_log.warn("L2SummonAI: onEvtArrived()");
/*
		if(_intention == AI_INTENTION_FOLLOW && _followTask == null)
		{
			if(debug)
				_log.warn("L2SummonAI: onEvtArrived() start follow");

			_followTask = ThreadPoolManager.getInstance().scheduleGeneral(new ThinkFollow(), 250);
			return;
		}
*/
		if(!setNextIntention())
			if(_intention == AI_INTENTION_INTERACT || _intention == AI_INTENTION_PICK_UP)
				onEvtThink();
			else
				setIntention(AI_INTENTION_ACTIVE, null, null);
	}

	@Override
	public void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if(debug)
			_log.warn("L2SummonAI: changeIntention -> " + intention + " " + arg0 + " " + arg1);

		// nothing to do if it does not CAST intention
		if(!(intention == AI_INTENTION_CAST || intention == AI_INTENTION_ATTACK))
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}

		// do nothing if next intention is same as current one.
		if(intention == _intention && arg0 == _intention_arg0 && arg1 == _intention_arg1)
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}

		// push current intention to stack
		if(_intention == AI_INTENTION_ATTACK && _intention != intention || (_intention == AI_INTENTION_FOLLOW && ((L2Summon) _actor).getFollowStatus()))
		{
			if(debug)
				_log.warn("L2SummonAI: changeIntention -> Saving current intention: " + _intention + " " + _intention_arg0 + " " + _intention_arg1);
			_interuptedIntentions.push(new IntentionCommand(_intention, _intention_arg0, _intention_arg1));
		}
		super.changeIntention(intention, arg0, arg1);
	}

	@Override
	public L2Summon getActor()
	{
		return (L2Summon) super.getActor();
	}

	private class RunOnAttacked implements Runnable
	{
		private L2Character _attacker;
		private long _lastAttack;

		public void run()
		{
			if(debug)
				_log.warn("L2SummonAI: runOnAttacked");
			if(_attacker != null && _actor.getPlayer() != null && _lastAttack + 20000 > System.currentTimeMillis() && (_intention == AI_INTENTION_FOLLOW || _intention == AI_INTENTION_IDLE || _intention == AI_INTENTION_ACTIVE))
			{
				if(!_actor.isMoving && _actor.isInRange(_attacker, 110))
				{
					stopFollow();
					
					if(debug)
						_log.warn("L2SummonAI: runOnAttacked try move");

					Location src;

					if(((L2Summon) _actor).getLastFollowPosition() != null)
						src = ((L2Summon) _actor).getLastFollowPosition();
					else
						src = _actor.getPlayer().getLoc();

					Location dst = Util.getPointInRadius(src, Rnd.get(80, 160), (int) Util.calculateAngleFrom(_attacker.getX(), _attacker.getY(), _actor.getPlayer().getX(), _actor.getPlayer().getY()) + Rnd.get(115, 155));
					Location loc = Util.getPointInRadius(src, Rnd.get(80, 160), (int) Util.calculateAngleFrom(_attacker.getX(), _attacker.getY(), _actor.getPlayer().getX(), _actor.getPlayer().getY()) + Rnd.get(205, 245));

					if(_attacker.getDistance(loc.getX(), loc.getY()) > _attacker.getDistance(dst.getX(), dst.getY()))
						dst = loc;

					_actor.moveToLocation(dst, 0, false);
				}
			}	
			else
			{
				if(debug)
					_log.warn("L2SummonAI: runOnAttacked stop task");
				
				_attacker = null;
				if(_runOnAttackedTask != null)
					_runOnAttackedTask.cancel(true);

				_runOnAttackedTask = null;
				_runOnAttacked = null;
			}
		}

		public void setAttacker(L2Character attacker)
		{
			_attacker = attacker;
			_lastAttack = System.currentTimeMillis();
		}
	}
}