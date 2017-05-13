package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2TerritoryOutpostInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 10.07.2010 20:32:48
 */
public class ConditionHasOutpost extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player player = (L2Player) env.character;
		return !(!player.isClanLeader() || !(player.getClan().getCamp() instanceof L2TerritoryOutpostInstance));
	}
}
