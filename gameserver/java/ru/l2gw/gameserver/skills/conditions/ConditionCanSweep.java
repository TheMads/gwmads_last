package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 13.07.2010 15:02:35
 */
public class ConditionCanSweep extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(env.target == null || !env.target.isMonster() || !env.target.isDead())
		{
			env.character.sendPacket(Msg.INVALID_TARGET);
			return false;
		}

		if(!((L2MonsterInstance) env.target).isSpoiled())
		{
			env.character.sendPacket(new SystemMessage(SystemMessage.SWEEPER_FAILED_TARGET_NOT_SPOILED));
			return false;
		}
		return true;
	}

}
