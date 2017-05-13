package ru.l2gw.gameserver.model.entity.siege.reinforce;

import javolution.util.FastMap;

/**
 * @author rage
 * @date 01.07.2009 16:55:56
 */
public class GuardPowerReinforce extends Reinforce
{
	private FastMap<Integer, Double> _mult;

	public GuardPowerReinforce(int id, int level, int siegeUnitId)
	{
		super(id, level, siegeUnitId);
	}

	public void setHpMult(int level, double mult)
	{
		if(_mult == null)
			_mult = new FastMap<Integer, Double>();
		_mult.put(level, mult);
	}

	public double getMultByLevel(int level)
	{
		if(_mult == null || _mult.get(level) == null)
			return 1.;
		return _mult.get(level);
	}

	public void setActive(boolean active)
	{ }

	public String getType()
	{
		return "GUARDPOWER";
	}
}
