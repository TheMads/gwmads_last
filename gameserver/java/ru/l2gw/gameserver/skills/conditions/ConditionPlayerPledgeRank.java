package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 05.08.2010 22:17:23
 */
public class ConditionPlayerPledgeRank extends Condition
{
	private final int _minRank;
	public ConditionPlayerPledgeRank(int min)
	{
		_minRank = min;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player player = (L2Player) env.character;
		return player.getPledgeRank() >= _minRank;
	}
}
