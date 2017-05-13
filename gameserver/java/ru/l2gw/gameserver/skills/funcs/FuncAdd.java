package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;

public class FuncAdd extends Func
{
	public FuncAdd(Stats stat, int order, Object owner, double value)
	{
		super(stat, order, owner, value);
	}

	@Override
	public void calc(Env env)
	{
		if(_cond == null || _cond.test(env))
			env.value += _value;
	}
}
