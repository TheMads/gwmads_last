package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author: ic
 * @date: 05.08.2010 21:31:27
 */
public class ConditionOpCloak extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		L2Player player = env.character.getPlayer();
		return player != null && player.isStatActive(Stats.CLOAK);
	}
}
