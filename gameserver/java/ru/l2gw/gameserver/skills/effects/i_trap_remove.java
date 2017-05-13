package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2TrapInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 20.11.2009 15:04:15
 */
public class i_trap_remove extends i_effect
{
	public i_trap_remove(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target instanceof L2TrapInstance && env.target.getLevel() <= getSkill().getMagicLevel() && ((L2TrapInstance) env.target).isDetected())
			{
				if(cha.isPlayer() && env.target.getPlayer() == cha || (env.target.getPlayer() != null && env.target.getPlayer().getParty() != null && env.target.getPlayer().getParty().containsMember(cha)))
					continue;

				env.target.doDie(null);
			}
	}
}
