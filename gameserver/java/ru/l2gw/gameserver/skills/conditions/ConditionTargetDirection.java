package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public class ConditionTargetDirection extends Condition
{
	public enum TargetDirection
	{
		FRONT,
		SIDE,
		BEHIND
	}

	private final TargetDirection _dir;

	public ConditionTargetDirection(TargetDirection direction)
	{
		_dir = direction;
	}

	@Override
	public boolean testImpl(Env env)
	{
		int head = env.character.getHeadingTo(env.target, true);

		if(head == -1)
			return false;

		if(head <= 10923 || head >= 54613)
			return _dir == TargetDirection.BEHIND;
		if(head >= 21845 && head <= 43691)
			return _dir == TargetDirection.FRONT;
		return _dir == TargetDirection.SIDE;

	}
}
