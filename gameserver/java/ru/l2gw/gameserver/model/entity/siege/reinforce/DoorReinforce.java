package ru.l2gw.gameserver.model.entity.siege.reinforce;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.l2gw.gameserver.tables.DoorTable;

/**
 * Created by IntelliJ IDEA.
 * User: admin
 * Date: 12.02.2009
 * Time: 16:38:18
 */
public class DoorReinforce extends Reinforce
{
	private FastList<Integer> _gates;
	private FastMap<Integer, Double> _hpMult;

	public DoorReinforce(int id, int level, int siegeUtintId)
	{
		super(id, level, siegeUtintId);
		_gates = new FastList<Integer>();
	}

	public void addGate(int doorId)
	{
		_gates.add(doorId);
	}

	public FastList<Integer> getGates()
	{
		return _gates;
	}
	
	@Override
	public String getType()
	{
		return "DOOR";
	}

	public void setHpMult(int level, double mult)
	{
		if(_hpMult == null)
			_hpMult = new FastMap<Integer, Double>();
		_hpMult.put(level, mult);
	}

	public double getMultByLevel(int level)
	{
		if(_hpMult == null || _hpMult.get(level) == null)
			return 1.;
		return _hpMult.get(level);
	}

	@Override
	public void setActive(boolean active)
	{
		_active = active;
		for(Integer doorId : _gates)
		{
			if(active)
				DoorTable.getInstance().getDoor(doorId).setHpMult(getMultByLevel(_level));
			else
				DoorTable.getInstance().getDoor(doorId).setHpMult(1);
		}
	}
}
