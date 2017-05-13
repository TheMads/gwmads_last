package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 05.08.2010 22:42:18
 */
public class ConditionPlayerPledgeType extends Condition
{
	private final int _pledgeType;
	public ConditionPlayerPledgeType(int pledgeType)
	{
		_pledgeType = pledgeType;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player player = (L2Player) env.character;
		return player.getClanId() > 0 && player.getPledgeType() == _pledgeType;
	}
}
