package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.skills.Env;

public class ConditionGameTime extends Condition
{
	public enum CheckGameTime
	{
		NIGHT
	}

	private final CheckGameTime _check;

	private final boolean _required;

	public ConditionGameTime(CheckGameTime check, boolean required)
	{
		_check = check;
		_required = required;
	}

	@Override
	public boolean testImpl(@SuppressWarnings("unused") Env env)
	{
		switch(_check)
		{
			case NIGHT:
				return GameTimeController.getInstance().isNowNight() == _required;
		}
		return !_required;
	}
}
