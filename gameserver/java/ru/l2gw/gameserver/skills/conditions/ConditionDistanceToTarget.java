package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.model.L2Character;

public class ConditionDistanceToTarget extends Condition
{

	private final int _validDistance;

	public ConditionDistanceToTarget(int distance)
	{
		_validDistance = distance;
	}

	@Override
	public boolean testImpl(Env env)
	{
		L2Character _target = env.target;
		L2Character _character = env.character;
		double _range = _character.getDistance(_target.getX(), _target.getY());
		if((_range > _validDistance - 50) && (_range < _validDistance + 50))
			return true;
		else
			return false;
	}
}
