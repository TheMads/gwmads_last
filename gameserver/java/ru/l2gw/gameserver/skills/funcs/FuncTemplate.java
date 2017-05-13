package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.conditions.Condition;
import ru.l2gw.gameserver.templates.StatsSet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class FuncTemplate
{
	public Condition _attachCond;
	public Condition _applyCond;
	public Class<?> _func;
	public Constructor<?> _constructor;
	public Stats _stat;
	public int _order;
	public double _value;
	public String _funcName;
	public StatsSet _attr;

	@SuppressWarnings("unchecked")
	public FuncTemplate(Condition attachCond, Condition applyCond, String func, Stats stat, int order, double value)
	{
		_attachCond = attachCond;
		_applyCond = applyCond;
		_stat = stat;
		_order = order;
		_value = value;
		_funcName = func;
		try
		{
			_func = Class.forName("ru.l2gw.gameserver.skills.funcs.Func" + func);
		}
		catch(ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		try
		{
			_constructor = _func.getConstructor(Stats.class, // stats to update
					Integer.TYPE, // order of execution
					Object.class, // owner
					Double.TYPE // value for function
			);
		}
		catch(NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
	}

	public Func getFunc(Env env, Object owner)
	{
		if(_attachCond != null && !_attachCond.test(env))
			return null;
		try
		{
			Func f = (Func) _constructor.newInstance(_stat, _order, owner, _value);
			f.setCharacter(env.character);
			if(_applyCond != null)
				f.setCondition(_applyCond);
			if(_attr != null)
				f.setAttributes(_attr);
			return f;
		}
		catch(IllegalAccessException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(InstantiationException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(InvocationTargetException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
