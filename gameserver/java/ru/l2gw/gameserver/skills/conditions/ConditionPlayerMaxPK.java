package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

public class ConditionPlayerMaxPK extends Condition
{
	private final int _pk;

	public ConditionPlayerMaxPK(int pk)
	{
		_pk = pk;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.character.isPlayer())
			return ((L2Player) env.character).getPkKills() <= _pk;
		return false;
	}
}