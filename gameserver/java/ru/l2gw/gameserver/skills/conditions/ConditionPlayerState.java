package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;


public class ConditionPlayerState extends Condition
{
	public enum CheckPlayerState
	{
		RESTING,
		MOVING,
		RUNNING,
		STANDING,
		FLYING,
		OLYMPIAD,
		SITING_ONLY
	}

	private final CheckPlayerState _check;

	private final boolean _required;

	public ConditionPlayerState(CheckPlayerState check, boolean required)
	{
		_check = check;
		_required = required;
	}

	@Override
	public boolean testImpl(Env env)
	{
		switch(_check)
		{
			case RESTING:
				if(env.character.isPlayer())
					return ((L2Player) env.character).isSitting() == _required;
				return !_required;
			case MOVING:
				return env.character.isMoving == _required;
			case RUNNING:
				return (env.character.isMoving && env.character.isRunning()) == _required;
			case STANDING:
				if(env.character.isPlayer())
					return ((L2Player) env.character).isSitting() != _required && env.character.isMoving != _required;
				return env.character.isMoving != _required;
			case FLYING:
				if(env.character.isPlayer())
					return env.character.isFlying() == _required;
				return !_required;
			case OLYMPIAD:
				if(env.character.isPlayer())
					return ((L2Player)env.character).isInOlympiadMode() == _required;
				return !_required;
			case SITING_ONLY:
				if(env.character.isPlayer())
					return ((L2Player) env.character).isSitting() == _required;
			return true;
		}
		return !_required;
	}
}
