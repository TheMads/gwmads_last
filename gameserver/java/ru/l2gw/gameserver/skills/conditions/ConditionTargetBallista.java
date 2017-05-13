package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.instances.L2BallistaInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 02.07.2009 12:50:04
 */
public class ConditionTargetBallista extends Condition
{
	private final boolean _ballista;

	public ConditionTargetBallista(boolean ballista)
	{
		_ballista = ballista;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target instanceof L2BallistaInstance == _ballista;
	}

}
