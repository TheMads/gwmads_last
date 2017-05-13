package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 02.08.2010 16:22:18
 */
public class ConditionPlayerKarma extends Condition
{
	private final boolean _karma;
	public ConditionPlayerKarma(boolean karma)
	{
		_karma = karma;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getKarma() > 0 == _karma;
	}
}
