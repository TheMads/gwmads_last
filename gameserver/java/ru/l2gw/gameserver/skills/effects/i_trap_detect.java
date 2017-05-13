package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2TrapInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 20.11.2009 14:37:43
 */
public class i_trap_detect extends i_effect
{
	public i_trap_detect(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target instanceof L2TrapInstance && env.target.getLevel() <= getSkill().getMagicLevel())
			{
				if(cha.isPlayer() && env.target.getPlayer() == cha || (env.target.getPlayer() != null && env.target.getPlayer().getParty() != null && env.target.getPlayer().getParty().containsMember(cha)))
					continue;

				((L2TrapInstance) env.target).setDetected((int) calc());
				((L2NpcInstance) env.target).notifyAiEvent(env.target, CtrlEvent.EVT_TRAP_DETECTED, cha, null, null);
			}
	}
}
