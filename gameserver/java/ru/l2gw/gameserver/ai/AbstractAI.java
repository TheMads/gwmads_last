package ru.l2gw.gameserver.ai;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.listeners.engine.DefaultListenerEngine;
import ru.l2gw.extensions.listeners.engine.ListenerEngine;
import ru.l2gw.extensions.listeners.events.AbstractAI.AbstractAINotifyEvent;
import ru.l2gw.gameserver.instancemanager.superpoint.SuperpointNode;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.util.Location;

import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

public abstract class AbstractAI
{
	protected static final Log _log = LogFactory.getLog(AbstractAI.class.getName());

	protected final L2Character _actor;

	protected CtrlIntention _intention = AI_INTENTION_IDLE;
	protected Object _intention_arg0 = null;
	protected Object _intention_arg1 = null;

	private L2Character _attack_target;
	protected L2Skill _skill;
	public boolean debug = false;

	protected AbstractAI(L2Character actor)
	{
		_actor = actor;
	}

	public void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if(debug)
			_log.info("AbstractAI: " + _actor + ".changeIntention(" + intention + ", " + arg0 + ", " + arg1 + ")");

		_intention = intention;
		_intention_arg0 = arg0;
		_intention_arg1 = arg1;
	}

	public final void setIntention(CtrlIntention intention)
	{
		setIntention(intention, null, null);
	}

	public final void setIntention(CtrlIntention intention, Object arg0)
	{
		setIntention(intention, arg0, null);
	}

	public void setIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if(!_actor.hasAI())
			return;

		if(!_actor.isVisible())
		{
			if(_intention == AI_INTENTION_IDLE)
				return;

			intention = AI_INTENTION_IDLE;
		}
		if(debug)
			_log.info(_actor + " setIntention: " + intention + " " + arg0 + " " + arg1);

		switch(intention)
		{
			case AI_INTENTION_IDLE:
				onIntentionIdle();
				break;
			case AI_INTENTION_ACTIVE:
				onIntentionActive();
				break;
			case AI_INTENTION_REST:
				onIntentionRest();
				break;
			case AI_INTENTION_ATTACK:
				if(arg0 instanceof L2Character)
					onIntentionAttack((L2Character) arg0);
				break;
			case AI_INTENTION_CAST:
				if(arg0 instanceof L2Skill && arg1 instanceof L2Character)
					onIntentionCast((L2Skill) arg0, (L2Character) arg1);
				break;
			case AI_INTENTION_PICK_UP:
				if(arg0 instanceof L2Object)
					onIntentionPickUp((L2Object) arg0);
				break;
			case AI_INTENTION_INTERACT:
				if(arg0 instanceof L2Object)
					onIntentionInteract((L2Object) arg0);
				break;
			case AI_INTENTION_FOLLOW:
				if(arg0 instanceof L2Character && arg1 instanceof Integer)
					onIntentionFollow((L2Character) arg0, (Integer) arg1);
				break;
		}
	}

	public final void notifyEvent(CtrlEvent evt)
	{
		notifyEvent(evt, null, null, null);
	}

	public final void notifyEvent(CtrlEvent evt, Object arg0)
	{
		notifyEvent(evt, arg0, null, null);
	}

	public final void notifyEvent(CtrlEvent evt, Object arg0, Object arg1)
	{
		notifyEvent(evt, arg0, arg1, null);
	}

	public void notifyEvent(CtrlEvent evt, Object arg0, Object arg1, Object arg2)
	{
		if((!_actor.isVisible() && evt != CtrlEvent.EVT_TIMER && evt != CtrlEvent.EVT_SCRIPT_EVENT) || !_actor.hasAI())
			return;

		getListenerEngine().fireMethodInvoked(new AbstractAINotifyEvent(MethodCollection.AbstractAInotifyEvent, this, new Object[] { evt, new Object[] { arg0, arg1, arg2 } }));

		switch(evt)
		{
			case EVT_THINK:
				onEvtThink();
				break;
			case EVT_ATTACKED:
				if(arg0 == null)
					return;
				onEvtAttacked((L2Character) arg0, ((Number) arg1).intValue(), (L2Skill) arg2);
				break;
			case EVT_CLAN_ATTACKED:
				onEvtClanAttacked((L2Character) arg0, (L2Character) arg1, ((Number) arg2).intValue());
				break;
			case EVT_AGGRESSION:
				if(arg0 == null)
					return;
				onEvtAggression((L2Character) arg0, ((Number) arg1).intValue(), (L2Skill) arg2);
				break;
			case EVT_MANIPULATION:
				onEvtManipulation((L2Character) arg0, ((Number) arg1).intValue(), (L2Skill) arg2);
				break;
			case EVT_READY_TO_ACT:
				onEvtReadyToAct();
				break;
			case EVT_ARRIVED:
				onEvtArrived();
				break;
			case EVT_ARRIVED_TARGET:
				onEvtArrivedTarget();
				break;
			case EVT_ARRIVED_BLOCKED:
				onEvtArrivedBlocked((Location) arg0);
				break;
			case EVT_FORGET_OBJECT:
				onEvtForgetObject((L2Object) arg0);
				break;
			case EVT_DEAD:
				onEvtDead(arg0 instanceof L2Character ? (L2Character) arg0 : null);
				break;
			case EVT_FAKE_DEATH:
				onEvtFakeDeath();
				break;
			case EVT_FINISH_CASTING:
				onEvtFinishCasting((L2Skill) arg0);
				break;
			case EVT_SEE_SPELL:
				onEvtSeeSpell((L2Skill) arg0, (L2Character) arg1);
				break;
			case EVT_SPAWN:
				onEvtSpawn();
				break;
			case EVT_TIMER:
				onEvtTimer(((Number) arg0).intValue(), arg1, arg2);
				break;
			case EVT_OUT_OF_MY_TERRITORY:
				onEvtOutOfMyTerritory();
				break;
			case EVT_SCRIPT_EVENT:
				onEvtScriptEvent(((Number) arg0).intValue(), arg1, arg2);
				break;
			case EVT_NODE_ARRIVED:
				onEvtNodeArrived((SuperpointNode) arg0);
				break;
			case EVT_SPELLED:
				onEvtSpelled((L2Skill) arg0, (L2Character) arg1);
				break;
			case EVT_SEE_CREATURE:
				onEvtSeeCreature((L2Character) arg0);
				break;
			case EVT_CREATURE_LOST:
				onEvtCreatureLost(arg0 instanceof L2Character ? (L2Character) arg0 : null, (Integer) arg1);
				break;
			case EVT_ABNORMAL_STATUS_CHANGED:
				onEvtAbnormalStatusChanged((L2Character) arg0, (L2Effect) arg1, (Boolean) arg2);
				break;
			case EVT_PARTY_DIED:
				onEvtPartyDead((L2NpcInstance) arg0);
				break;
			case EVT_CLAN_DIED:
				onEvtClanDead((L2NpcInstance) arg0);
				break;
			case EVT_DIE_SET:
				onEvtDieSet((L2Character) arg0);
				break;
			case EVT_PARTY_ATTACKED:
				onEvtPartyAttacked((L2Character) arg0, (L2Character) arg1, (Integer) arg2);
				break;
			case EVT_NO_DESIRE:
				onEvtNoDesire();
				break;
			case EVT_TRAP_STEP_IN:
				onEvtTrapStepIn((L2Character) arg0);
				break;
			case EVT_TRAP_STEP_OUT:
				onEvtTrapStepOut((L2Character) arg0);
				break;
			case EVT_TRAP_ACTIVATED:
				onEvtTrapActivated();
				break;
			case EVT_TRAP_DETECTED:
				onEvtTrapDetected((L2Character) arg0);
				break;
			case EVT_TRAP_DEFUSED:
				onEvtTrapDefused((L2Character) arg0);
				break;
		}
	}

	protected void clientActionFailed()
	{
		if(_actor.isPlayer())
			_actor.sendActionFailed();
	}

	public void clientStopMoving()
	{
		_actor.stopMove();
	}

	public L2Character getActor()
	{
		return _actor;
	}

	public CtrlIntention getIntention()
	{
		return _intention;
	}

	public void setAttackTarget(L2Character target)
	{
		_attack_target = target;
	}

	public L2Character getAttackTarget()
	{
		return _attack_target;
	}

	/** Означает, что AI всегда включен, независимо от состояния региона */
	public boolean isGlobalAI()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public void setGlobalAggro(int value)
	{}

	@Override
	public String toString()
	{
		return getL2ClassShortName() + " for " + _actor;
	}

	public String getL2ClassShortName()
	{
		if(getClass() != null && getClass().getName() != null)
			return getClass().getName().replaceAll("^.*\\.(.*?)$", "$1");
		return "";
	}

	protected abstract void onIntentionIdle();

	protected abstract void onIntentionActive();

	protected abstract void onIntentionRest();

	protected abstract void onIntentionAttack(L2Character target);

	protected abstract void onIntentionCast(L2Skill skill, L2Character target);

	protected abstract void onIntentionPickUp(L2Object item);

	protected abstract void onIntentionInteract(L2Object object);

	protected abstract void onEvtThink();

	protected abstract void onEvtAttacked(L2Character attacker, int damage, L2Skill skill);

	protected abstract void onEvtClanAttacked(L2Character attacked_member, L2Character attacker, int damage);

	protected abstract void onEvtAggression(L2Character target, int aggro, L2Skill skill);

	protected abstract void onEvtManipulation(L2Character target, int aggro, L2Skill skill);

	protected abstract void onEvtReadyToAct();

	protected abstract void onEvtArrived();

	protected abstract void onEvtArrivedTarget();

	protected abstract void onEvtArrivedBlocked(Location blocked_at_pos);

	protected abstract void onEvtForgetObject(L2Object object);

	protected abstract void onEvtDead(L2Character killer);

	protected abstract void onEvtFakeDeath();

	protected abstract void onEvtFinishCasting(L2Skill skill);

	protected abstract void onEvtSeeSpell(L2Skill skill, L2Character caster);

	protected abstract void onEvtSpawn();

	protected abstract void onEvtTimer(int timerId, Object arg1, Object arg2);

	protected abstract void onEvtOutOfMyTerritory();

	protected abstract void onEvtScriptEvent(int eventId, Object arg1, Object arg2);

	protected abstract void onIntentionFollow(L2Character target, Integer offset);

	protected abstract void onEvtNodeArrived(SuperpointNode node);

	protected abstract void onEvtSpelled(L2Skill skill, L2Character caster);

	protected abstract void onEvtSeeCreature(L2Character creature);

	protected abstract void onEvtCreatureLost(L2Character creature, int objectId);

	protected abstract void onEvtAbnormalStatusChanged(L2Character speller, L2Effect effect, boolean added);

	protected abstract void onEvtPartyDead(L2NpcInstance partyPrivate);

	protected abstract void onEvtDieSet(L2Character talker);

	protected abstract void onEvtClanDead(L2NpcInstance clanPrivate);

	protected abstract void onEvtPartyAttacked(L2Character attacker, L2Character victim, int damage);

	protected abstract void onEvtNoDesire();

	protected abstract void onEvtTrapStepIn(L2Character cha);

	protected abstract void onEvtTrapStepOut(L2Character cha);

	protected abstract void onEvtTrapActivated();

	protected abstract void onEvtTrapDetected(L2Character cha);

	protected abstract void onEvtTrapDefused(L2Character cha);

	public abstract boolean onTalk(L2Player talker);

	public abstract void onTalkSelected(L2Player talker, int choice, boolean from_choice);

	public abstract void onMenuSelected(L2Player talker, int ask, int reply);

	public abstract void classChangeRequested(L2Player talker, int occupation_name_id);

	public abstract void onTeleportRequested(L2Player talker);

	public abstract void onTeleport(L2Player talker, int listId, int listPos, int itemId);

	private ListenerEngine<AbstractAI> listenerEngine;

	public ListenerEngine<AbstractAI> getListenerEngine()
	{
		if(listenerEngine == null)
			listenerEngine = new DefaultListenerEngine<AbstractAI>(this);
		return listenerEngine;
	}
}
