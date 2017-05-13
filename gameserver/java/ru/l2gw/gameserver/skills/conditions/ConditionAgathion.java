package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 25.11.2009 15:13:00
 */
public class ConditionAgathion extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		return env.character.isPlayer() && ((L2Player) env.character).getAgathionId() != 0;
	}
}
