package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 15.07.2010 12:24:40
 */
public class ConditionFishingReeling extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		if(!((L2Player) env.character).isFishing())
		{
			env.character.sendPacket(Msg.REELING_SKILL_IS_AVAILABLE_ONLY_WHILE_FISHING);
			return false;
		}
		return true;
	}
}
