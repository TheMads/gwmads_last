package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public class ConditionPlayerSubJob extends Condition
{

	boolean _isSubClassActive;

	public ConditionPlayerSubJob(boolean isSubClassActive)
	{
		_isSubClassActive = isSubClassActive;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.isPlayer() && env.character.getPlayer().isSubClassActive() == _isSubClassActive;
	}

}
