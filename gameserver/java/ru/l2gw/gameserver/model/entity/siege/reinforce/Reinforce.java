package ru.l2gw.gameserver.model.entity.siege.reinforce;

import javolution.util.FastMap;
import ru.l2gw.database.mysql;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: admin
 * Date: 12.02.2009
 * Time: 16:32:25
 */
public abstract class Reinforce
{
	private int _id;
	protected int _siegeUnitId;
	private String _name;
	protected int _level;
	private int _maxLevel = 0;
	protected boolean _active = false;
	private Map<Integer, Integer> _price;

	public Reinforce(int id, int level, int siegeUnitId)
	{
		_id = id;
		_level = level;
		_siegeUnitId = siegeUnitId;
		_price = new FastMap<Integer, Integer>();
	}

	public Reinforce(int id, int level, String name)
	{
		_id = id;
		_level = level;
		_name = name;
	}

	public int getId()
	{
		return _id;
	}

	public int getLevel()
	{
		return _level;
	}

	public void setLevel(int level)
	{
		_level = level;
	}

	public String getName()
	{
		return _name;
	}

	public void setName(String name)
	{
		_name = name;
	}

	public abstract String getType();

	public int getPrice(int level)
	{
		return _price.get(level);
	}

	public void setPrice(int level, int price)
	{
		_price.put(level, price);
		if(level > _maxLevel)
			_maxLevel = level;
	}

	public int getSiegeUnitId()
	{
		return _siegeUnitId;
	}

	public void store()
	{
		if(_level > 0)
			mysql.set("REPLACE INTO siege_reinforce VALUES(" + _siegeUnitId + ", " + _id + ", " + _level + ")");
		else
			mysql.set("DELETE FROM siege_reinforce WHERE siegeUnitId=" + _siegeUnitId + " and reinforceId=" + _id);
	}

	public int getMaxLevel()
	{
		return _maxLevel;
	}

	public boolean isActive()
	{
		return _active;
	}

	public abstract void setActive(boolean active);
}
