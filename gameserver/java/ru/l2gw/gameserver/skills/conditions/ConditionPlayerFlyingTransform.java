package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 08.11.2010 18:46:20
 */
public class ConditionPlayerFlyingTransform extends Condition
{
	private final boolean _has;

	public ConditionPlayerFlyingTransform(boolean has)
	{
		_has = has;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.isPlayer() && ((L2Player) env.character).isInFlyingTransform() == _has;
	}
}
