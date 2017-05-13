package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public class ConditionTargetMyParty extends Condition
{
	private final boolean _isInMyParty;

	public ConditionTargetMyParty(boolean isInMyParty)
	{
		_isInMyParty = isInMyParty;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character == env.target.getPlayer() || (env.character.getPlayer() != null && env.character.getPlayer().getParty() != null && env.character.getPlayer().getParty().containsMember(env.target)) == _isInMyParty;
	}
}
