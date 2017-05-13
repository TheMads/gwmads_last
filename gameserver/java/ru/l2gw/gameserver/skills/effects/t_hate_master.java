package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;

import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

public class t_hate_master extends t_effect
{
	public t_hate_master(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(getEffected() instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) getEffected();
			summon.setPossessed(true);
			summon.getAI().setAttackTarget(summon.getPlayer());
			summon.getAI().Attack(summon.getPlayer(), true, false);
		}
		else if(getEffected().isMinion())
		{
			L2NpcInstance minion = (L2NpcInstance) getEffected();
			minion.stopHate();
			minion.getAI().notifyEvent(CtrlEvent.EVT_MANIPULATION, minion.getLeader(), 50000);
			minion.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, minion.getLeader());
		}
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(getEffected() instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) getEffected();
			summon.setPossessed(false);
			summon.getAI().setIntention(AI_INTENTION_IDLE);
		}
		else if(getEffected().isMinion())
		{
			L2NpcInstance minion = (L2NpcInstance) getEffected();
			minion.stopHate();
			minion.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
	}

	@Override
	public boolean isSuccess(boolean skillSuccess)
	{
		return !getEffected().isPlayer() && super.isSuccess(skillSuccess);
	}
}