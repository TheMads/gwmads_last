package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.util.Location;

public class ConditionMinDistanceToTarget extends Condition
{

	private final int _minValidDistance;

	public ConditionMinDistanceToTarget(int minDistance)
	{
		_minValidDistance = minDistance;
	}

	@Override
	public boolean testImpl(Env env)
	{
		Location loc = env.character.getPrevLoc() == null ? env.character.getLoc() : env.character.getPrevLoc();
		return env.target.getDistance(loc.getX(), loc.getY()) > _minValidDistance;
	}
}
