package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;

public class FuncMul extends Func
{
	public FuncMul(Stats stat, int order, Object owner, double value)
	{
		super(stat, order, owner, value);
	}

	@Override
	public void calc(Env env)
	{
		if(_cond == null || _cond.test(env))
		{
			//if(_stat == Stats.CRITICAL_RATE)
			//	env.value += env.baseValue * (_value - 1);
			//else
				env.value *= _value;
		}
	}
}
