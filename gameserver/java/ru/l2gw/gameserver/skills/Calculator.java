package ru.l2gw.gameserver.skills;

import ru.l2gw.gameserver.skills.funcs.Func;

/**
 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...).
 * In fact, each calculator is a table of Func object in which each Func represents a mathematic function : <BR><BR>
 *
 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR><BR>
 *
 * When the calc method of a calculator is launched, each mathematic function is called according to its priority <B>_order</B>.
 * Indeed, Func with lowest priority order is executed firsta and Funcs with the same order are executed in unspecified order.
 * The result of the calculation is stored in the value property of an Env class instance.<BR><BR>
 *
 * Method addFunc and removeFunc permit to add and remove a Func object from a Calculator.<BR><BR>
 *
 */
public final class Calculator
{
	/** Empty Func table definition */
	static final Func[] emptyFuncs = new Func[0];

	/** Table of Func object */
	private Func[] _functions;

	/**
	 * Constructor of Calculator (Init value : emptyFuncs).<BR><BR>
	 */
	public Calculator()
	{
		_functions = emptyFuncs;
	}

	/**
	 * Return the number of Funcs in the Calculator.<BR><BR>
	 */
	public int size()
	{
		return _functions.length;
	}

	/**
	 * Add a Func to the Calculator.<BR><BR>
	 */
	public synchronized void addFunc(Func f)
	{
		Func[] funcs = _functions;
		Func[] tmp = new Func[funcs.length + 1];

		final int order = f._order;
		int i;

		for(i = 0; i < funcs.length && order >= funcs[i]._order; i++)
			tmp[i] = funcs[i];

		tmp[i] = f;

		for(; i < funcs.length; i++)
			tmp[i + 1] = funcs[i];

		_functions = tmp;
	}

	/**
	 * Remove a Func from the Calculator.<BR><BR>
	 */
	public synchronized void removeFunc(Func f)
	{
		Func[] funcs = _functions;
		Func[] tmp = new Func[funcs.length - 1];

		int i;

		for(i = 0; i < funcs.length && f != funcs[i]; i++)
			tmp[i] = funcs[i];

		if(i == funcs.length)
			return;

		for(i++; i < funcs.length; i++)
			tmp[i - 1] = funcs[i];

		if(tmp.length == 0)
			_functions = emptyFuncs;
		else
			_functions = tmp;

	}

	/**
	 * Remove each Func with the specified owner of the Calculator.<BR><BR>
	 */
	public synchronized void removeOwner(Object owner)
	{
		Func[] funcs = _functions;

		for(Func element : funcs)
			if(element._funcOwner == owner)
				removeFunc(element);
	}

	/**
	 * Run each Func of the Calculator.<BR><BR>
	 */
	public void calc(Env env)
	{
		Func[] funcs = _functions;

		for(Func element : funcs)
			element.calc(env);
	}

	public Func[] getFunctions()
	{
		return _functions;
	}

	/*
	@Override
	public String toString()
	{
		String ret = "Calculator: ";
		for(Func f : _functions)
			ret += f + ";";
		return ret;
	}
	*/
}
