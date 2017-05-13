package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.conditions.Condition;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * A Func object is a component of a Calculator created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...).
 * In fact, each calculator is a table of Func object in which each Func represents a mathematic function : <BR><BR>
 *
 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR><BR>
 *
 * When the calc method of a calculator is launched, each mathematic function is called according to its priority <B>_order</B>.
 * Indeed, Func with lowest priority order is executed firsta and Funcs with the same order are executed in unspecified order.
 * The result of the calculation is stored in the value property of an Env class instance.<BR><BR>
 *
 */

public abstract class Func
{
	/** Statistics, that is affected by this function (See L2Character.CALCULATOR_XXX constants) */
	public final Stats _stat;

	/**
	 * Order of functions calculation.
	 * Functions with lower order are executed first.
	 * Functions with the same order are executed in unspecified order.
	 * Usually add/substruct functions has lowest order,
	 * then bonus/penalty functions (multiplay/divide) are
	 * applied, then functions that do more complex calculations
	 * (non-linear functions).
	 */
	public final int _order;

	/**
	 *  Owner can be an armor, weapon, skill, system event, quest, etc
	 *  Used to remove all functions added by this owner.
	 */
	public final Object _funcOwner;
	protected Condition _cond;
	public final double _value;

	public Func(Stats stat, int order, Object owner)
	{
		this(stat, order, owner, 0);
	}

	public Func(Stats stat, int order, Object owner, double value)
	{
		_stat = stat;
		_order = order;
		_funcOwner = owner;
		_value = value;
	}

	public void setCondition(Condition cond)
	{
		_cond = cond;
	}

	public void setAttributes(StatsSet set)
	{}

	public void setCharacter(L2Character cha)
	{}

	public abstract void calc(Env env);

	/**
	 * Для отладки
	 *
	public Condition getCondition()
	{
		return _cond;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[stat=" + _stat + ";value=" + _value + ";owner=" + _funcOwner + "]";
	}
	*/
}
