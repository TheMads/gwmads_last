package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.taskmanager.DecayTaskManager;

/**
 * @author rage
 * @date 23.11.2009 13:11:45
 */
public class ConditionConsumeBody extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.first)
			return true;

		if(env.target.isRaid())
			return false;

		boolean ret = env.target != null && env.target.isDead() && DecayTaskManager.getInstance().getDecayTime(env.target) > System.currentTimeMillis() + env.target.getTemplate().corpse_time / 2;

		if(!ret)
			env.character.sendPacket(new SystemMessage(SystemMessage.THE_CORPSE_IS_TOO_OLD_THE_SKILL_CANNOT_BE_USED));

		return ret;
	}
}
