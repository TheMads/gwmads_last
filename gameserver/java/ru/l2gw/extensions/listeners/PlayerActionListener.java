package ru.l2gw.extensions.listeners;

import ru.l2gw.extensions.listeners.events.MethodEvent;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;

/**
 * @author rage
 * @date 10.08.2010 16:43:54
 */
public class PlayerActionListener implements MethodInvokeListener, MethodCollection
{
	@Override
	public boolean accept(MethodEvent event)
	{
		return event.getMethodName().equals(onStartAttack) || event.getMethodName().equals(onStartCast) || event.getMethodName().equals(onStartAltCast) || event.getMethodName().equals(ReduceCurrentHp) || event.getMethodName().equals(onEffectAdd)
				|| event.getMethodName().equals(onActionRequest) || event.getMethodName().equals(onMoveRequest) || event.getMethodName().equals(onSkillUse) || event.getMethodName().equals(onTradeStart);
	}

	@Override
	public void methodInvoked(MethodEvent e)
	{
		if(e.getMethodName().equals(onStartCast))
		{
			L2Skill skill = (L2Skill) e.getArgs()[0];
			if(skill.getId() == 2099)
				return;
		}
		((L2Player) e.getOwner()).stopNonAggroTask();
	}
}
