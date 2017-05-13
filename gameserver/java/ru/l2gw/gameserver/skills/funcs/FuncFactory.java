package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.skills.Stats;

import java.lang.reflect.Constructor;

public class FuncFactory
{
	private final static FuncFactory _instance = new FuncFactory();

	private FuncFactory()
	{}

	public static FuncFactory getInstance()
	{
		return _instance;
	}

	public static Func createFunc(String func, Stats stat, int order, double lambdaValue) throws Exception
	{
		return createFunc(func, stat, order, lambdaValue, _instance);
	}

	@SuppressWarnings("unchecked")
	public static Func createFunc(String func, Stats stat, int order, double value, Object owner) throws Exception
	{
		Class<?> funcClass = Class.forName("ru.l2gw.gameserver.skills.funcs.Func" + func);
		Constructor<?> funcConstructor = funcClass.getConstructor(Stats.class, Integer.TYPE, Object.class, Double.TYPE);
		return (Func) funcConstructor.newInstance(stat, order, owner, value);
	}
}
