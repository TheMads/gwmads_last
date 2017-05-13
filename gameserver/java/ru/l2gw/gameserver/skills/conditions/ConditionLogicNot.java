package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public class ConditionLogicNot extends Condition
{
	private final Condition _condition;

	public ConditionLogicNot(Condition condition)
	{
		_condition = condition;
		if(getListener() != null)
			_condition.setListener(this);
	}

	@Override
	public void setListener(ConditionListener listener)
	{
		if(listener != null)
			_condition.setListener(this);
		else
			_condition.setListener(null);
		super.setListener(listener);
	}

	@Override
	public boolean testImpl(Env env)
	{
		return !_condition.test(env);
	}
}
