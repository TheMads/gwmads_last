package ru.l2gw.gameserver.ai;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.ai.L2PlayableAI.nextAction;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.superpoint.SuperpointNode;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.Die;
import ru.l2gw.util.Location;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static ru.l2gw.gameserver.ai.CtrlIntention.*;

public class L2CharacterAI extends AbstractAI
{
	private Map<Integer, ScheduledFuture<?>> _timers;
	public boolean superPointDirection = true;

	public L2CharacterAI(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onIntentionIdle()
	{
		setAttackTarget(null);
		clientStopMoving();
		changeIntention(AI_INTENTION_IDLE, null, null);
	}

	@Override
	protected void onIntentionActive()
	{
		setAttackTarget(null);
		clientStopMoving();
		changeIntention(AI_INTENTION_ACTIVE, null, null);
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{
		if(debug)
			_log.info(_actor + " onIntenetionAttack: " + target);
		setAttackTarget(target);
		changeIntention(AI_INTENTION_ATTACK, target, null);
		onEvtThink();
	}

	@Override
	protected void onIntentionCast(L2Skill skill, L2Character target)
	{
		_skill = skill;
		setAttackTarget(target);
		changeIntention(AI_INTENTION_CAST, skill, target);
		onEvtThink();
	}

	@Override
	protected void onIntentionFollow(L2Character target, Integer offset)
	{
		changeIntention(AI_INTENTION_FOLLOW, target, offset);
		_actor.followToCharacter(target, offset);
	}

	@Override
	protected void onEvtArrivedBlocked(Location blocked_at_pos)
	{
		_actor.stopMove();
		onEvtThink();
	}

	@Override
	protected void onEvtForgetObject(L2Object object)
	{
		if(_actor == null || object == null)
			return;

		if(_actor instanceof L2NpcInstance)
			((L2NpcInstance) _actor).removeNeighbor(object);

		if(getAttackTarget() == object || _actor.getTargetId() == object.getObjectId())
		{
			setAttackTarget(null);
			if(_actor.getTargetId() == object.getObjectId())
				_actor.setTarget(null);
			setIntention(AI_INTENTION_ACTIVE, null, null);
		}
	}

	@Override
	protected void onEvtDead(L2Character killer)
	{
		_actor.breakAttack();
		_actor.breakCast(true, false);
		_actor.stopMove();
		_actor.broadcastPacket(new Die(_actor));
		_intention = AI_INTENTION_IDLE;
		setAttackTarget(null);
	}

	@Override
	protected void onEvtFakeDeath()
	{
		clientStopMoving();
		_intention = AI_INTENTION_IDLE;
		setAttackTarget(null);
		setAttackTarget(null);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage, L2Skill skill)
	{
		_actor.startAttackStanceTask();
		if(attacker != null)
			attacker.startAttackStanceTask();
	}

	@Override
	protected void onEvtClanAttacked(L2Character attacked_member, L2Character attacker, int damage)
	{ }

	@SuppressWarnings("unused")
	public void Attack(L2Object target, boolean forceUse, boolean dontMove)
	{
		setIntention(AI_INTENTION_ATTACK, target);
	}

	public void Cast(L2Skill skill, L2Character target)
	{
		Cast(skill, target, null, false, false);
	}

	@SuppressWarnings("unused")
	public void Cast(L2Skill skill, L2Character target, L2ItemInstance usedItem, boolean forceUse, boolean dontMove)
	{
		setIntention(AI_INTENTION_ATTACK, target);
	}

	@Override
	protected void onEvtThink()
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro, L2Skill skill)
	{}

	@Override
	protected void onEvtManipulation(L2Character target, int aggro, L2Skill skill)
	{
		onEvtAggression(target, aggro, skill);
	}

	@Override
	protected void onEvtFinishCasting(L2Skill skill)
	{}

	@Override
	protected void onIntentionInteract(L2Object object)
	{}

	@Override
	protected void onIntentionPickUp(L2Object item)
	{}

	@Override
	protected void onEvtReadyToAct()
	{}

	@Override
	protected void onEvtArrived()
	{}

	@Override
	protected void onEvtArrivedTarget()
	{}

	@Override
	protected void onIntentionRest()
	{}

	@Override
	protected void onEvtSeeSpell(L2Skill skill, L2Character caster)
	{}

	@Override
	protected void onEvtSpawn()
	{}

	@Override
	protected void onEvtTimer(int timerId, Object arg1, Object arg2)
	{}

	@Override
	protected void onEvtOutOfMyTerritory()
	{}

	@Override
	protected void onEvtScriptEvent(int eventId, Object arg1, Object arg2)
	{}

	@Override
	protected void onEvtNodeArrived(SuperpointNode node)
	{}

	@Override
	protected void onEvtSpelled(L2Skill skill, L2Character caster)
	{}

	@Override
	protected void onEvtSeeCreature(L2Character creature)
	{}

	@Override
	protected void onEvtCreatureLost(L2Character creature, int objectId)
	{}

	@Override
	protected void onEvtAbnormalStatusChanged(L2Character speller, L2Effect effect, boolean added)
	{}

	@SuppressWarnings("unused")
	public void stopAITask()
	{}

	@SuppressWarnings("unused")
	public void startAITask()
	{}

	@SuppressWarnings("unused")
	public void setNextAction(nextAction action, Object arg0, Object arg1, boolean arg2, boolean arg3)
	{}

	@SuppressWarnings("unused")
	public void clearNextAction()
	{}

	@SuppressWarnings("unused")
	public void teleportHome()
	{}

	@SuppressWarnings("unused")
	public boolean checkAggression(L2Character target)
	{
		return false;
	}

	@Override
	public boolean onTalk(L2Player player)
	{
		return false;
	}

	@Override
	public void onTalkSelected(L2Player talker, int choice, boolean from_choice)
	{}

	@Override
	public void onMenuSelected(L2Player talker, int ask, int reply)
	{}

	@Override
	public void classChangeRequested(L2Player talker, int occupation_name_id)
	{}

	@Override
	public void onTeleportRequested(L2Player talker)
	{}

	@Override
	public void onTeleport(L2Player talker, int listId, int listPos, int itemId)
	{}

	@Override
	protected void onEvtPartyDead(L2NpcInstance partyPrivate)
	{}

	@Override
	protected void onEvtClanDead(L2NpcInstance clanPrivate)
	{}

	@Override
	protected void onEvtDieSet(L2Character talker)
	{}

	@Override
	protected void onEvtPartyAttacked(L2Character attacker, L2Character victim, int damage)
	{}

	@Override
	protected void onEvtNoDesire()
	{}

	@Override
	protected void onEvtTrapStepIn(L2Character cha)
	{}

	@Override
	protected void onEvtTrapStepOut(L2Character cha)
	{}

	@Override
	protected void onEvtTrapActivated()
	{}

	@Override
	protected void onEvtTrapDetected(L2Character cha)
	{}

	@Override
	protected void onEvtTrapDefused(L2Character cha)
	{}

	public boolean isActive()
	{
		return true;
	}

	public boolean isGlobalAggro()
	{
		return true;
	}

	public boolean inMyTerritory()
	{
		return true;
	}

	public void removeAttackDesire(L2Character target)
	{}

	public void broadcastScriptEvent(int eventId, Object arg1, Object arg2, int range)
	{
		for(L2NpcInstance npc : _actor.getKnownNpc(range))
			if(npc.hasAI())
				ThreadPoolManager.getInstance().executeAi(new L2ObjectTasks.NotifyAITask(npc, CtrlEvent.EVT_SCRIPT_EVENT, eventId, arg1, arg2), false);

		ThreadPoolManager.getInstance().executeAi(new L2ObjectTasks.NotifyAITask(_actor, CtrlEvent.EVT_SCRIPT_EVENT, eventId, arg1, arg2), false);
	}

	public void sendScriptEvent(L2Character target, int eventId, Object arg1, Object arg2)
	{
		if(target == null)
			return;

		ThreadPoolManager.getInstance().scheduleAi(new L2ObjectTasks.NotifyAITask(target, CtrlEvent.EVT_SCRIPT_EVENT, eventId, arg1, arg2), 2000, false);
	}

	public void addTimer(int timerId, long delay)
	{
		addTimer(timerId, null, null, delay);
	}

	public void addTimer(int timerId, Object arg1, long delay)
	{
		addTimer(timerId, arg1, null, delay);
	}

	public void addTimer(int timerId, Object arg1, Object arg2, long delay)
	{
		if(_timers == null)
			_timers = new ConcurrentHashMap<>();

		ScheduledFuture<?> timer = ThreadPoolManager.getInstance().scheduleAi(new Timer(timerId, arg1, arg2, delay), delay, false);
		if(timer != null)
		{
			ScheduledFuture<?> old = _timers.put(timerId, timer);
			if(old != null)
				old.cancel(true);
		}
	}

	public void addTimer(Timer timer)
	{
		if(_timers == null)
			_timers = new ConcurrentHashMap<>();

		ScheduledFuture<?> old = _timers.put(timer._timerId, ThreadPoolManager.getInstance().scheduleAi(timer, timer.delay, false));
		if(old != null)
			old.cancel(true);
	}

	public void blockTimer(int timerId)
	{
		if(_timers == null)
			return;

		ScheduledFuture<?> timer = _timers.remove(timerId);
		if(timer != null)
			timer.cancel(true);
	}

	public class Timer implements Runnable
	{
		public final int _timerId;
		public final Object _arg1;
		public final Object _arg2;
		public long delay;

		public Timer(int timerId, Object arg1, Object arg2, long d)
		{
			_timerId = timerId;
			_arg1 = arg1;
			_arg2 = arg2;
			delay = d;
		}

		public void run()
		{
			if(_timers != null)
				_timers.remove(_timerId);
			notifyEvent(CtrlEvent.EVT_TIMER, _timerId, _arg1, _arg2);
		}

		@Override
		public String toString()
		{
			return "Timer[" + _timerId + ";" + _arg1 + ";" + _arg2 + ";" + delay + ";" + _actor + ";" + _actor.getObjectId() + "]";
		}
	}

	public GArray<DefaultAI.Task> getTaskList()
	{
		return new GArray<>(0);
	}
}