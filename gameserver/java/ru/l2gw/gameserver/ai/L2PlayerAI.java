package ru.l2gw.gameserver.ai;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_REST;

public class L2PlayerAI extends L2PlayableAI
{
	public L2PlayerAI(L2Player actor)
	{
		super(actor);
	}

	@Override
	protected void onIntentionRest()
	{
		changeIntention(AI_INTENTION_REST, null, null);
		setAttackTarget(null);
		clientStopMoving();
	}

	@Override
	protected void onIntentionActive()
	{
		changeIntention(AI_INTENTION_IDLE, null, null);
	}

	@Override
	public void onIntentionInteract(L2Object object)
	{
		if(getActor().getSittingTask())
		{
			setNextAction(nextAction.INTERACT, object, null, false, false);
			return;
		}
		else if(getActor().isSitting())
		{
			getActor().sendPacket(Msg.YOU_CANNOT_MOVE_WHILE_SITTING);
			clientActionFailed();
			return;
		}
		super.onIntentionInteract(object);
	}

	@Override
	public void onIntentionPickUp(L2Object object)
	{
		if(getActor().getSittingTask())
		{
			setNextAction(nextAction.PICKUP, object, null, false, false);
			return;
		}
		else if(getActor().isSitting())
		{
			getActor().sendPacket(Msg.YOU_CANNOT_MOVE_WHILE_SITTING);
			clientActionFailed();
			return;
		}
		super.onIntentionPickUp(object);
	}

	@Override
	public void Attack(L2Object target, boolean forceUse, boolean dontMove)
	{
		if(getActor().getSittingTask())
		{
			setNextAction(nextAction.ATTACK, target, null, forceUse, dontMove);
			return;
		}
		else if(getActor().isSitting())
		{
			getActor().sendPacket(Msg.YOU_CANNOT_MOVE_WHILE_SITTING);
			clientActionFailed();
			return;
		}
		super.Attack(target, forceUse, dontMove);
	}

	@Override
	public void Cast(L2Skill skill, L2Character target, L2ItemInstance usedItem, boolean forceUse, boolean dontMove)
	{
		if(debug)
    		_log.info(_actor + " PlayerAI Cast --> " + target + " " + skill);
		if(!skill.altUse())
			// Если в этот момент встаем, то использовать скилл когда встанем
			if(getActor().getSittingTask())
			{
				setNextAction(nextAction.CAST, skill, target, forceUse, dontMove);
				clientActionFailed();
				return;
			}
			// если сидим - скиллы нельзя использовать
			else if(getActor().isSitting() || getActor().isFakeDeath())
			{
				getActor().sendPacket(Msg.YOU_CANNOT_MOVE_WHILE_SITTING);
				clientActionFailed();
				return;
			}
		super.Cast(skill, target, usedItem, forceUse, dontMove);
	}

	@Override
	public L2Player getActor()
	{
		return (L2Player) super.getActor();
	}
}