package ru.l2gw.gameserver.templates;

import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.tables.SkillTable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mkizub
 * <BR>
 * This class is used in order to have a set of couples (key,value).<BR>
 * Methods deployed are accessors to the set (add/get value from its key) and addition of a whole set in the current one.
 */
public final class StatsSet
{
	private final HashMap<String, Object> _set = new HashMap<String, Object>();

	public StatsSet()
	{}

	public StatsSet(Map<String, Object> map)
	{
		for(String key : map.keySet())
			_set.put(key, map.get(key));
	}

	/**
	 * Returns the set of values
	 * @return HashMap
	 */
	public final HashMap<String, Object> getSet()
	{
		return _set;
	}

	public Object getObject(String name)
	{
		return _set.get(name);
	}

	/**
	 * Return the boolean associated to the key put in parameter ("name")
	 * @param name : String designating the key in the set
	 * @return boolean : value associated to the key
	 */
	public boolean getBool(String name)
	{
		Object val = _set.get(name);
		if(val == null)
			throw new IllegalArgumentException("Boolean value required, but not specified");
		if(val instanceof Boolean)
			return (Boolean) val;
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Boolean value required, but found: " + val);
		}
	}

	/**
	 * Return the boolean associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : boolean designating the default value if value associated with the key is null
	 * @return boolean : value of the key
	 */
	public boolean getBool(String name, boolean deflt)
	{
		Object val = _set.get(name);
		if(val == null)
			return deflt;
		if(val instanceof Boolean)
			return ((Boolean) val).booleanValue();
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Boolean value required, but found: " + val);
		}
	}

	/**
	 * Returns the int associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return int : value associated to the key
	 */
	public int getInteger(String name)
	{
		Object val = _set.get(name);
		if(val == null)
			throw new IllegalArgumentException("Integer value required, but not specified");
		if(val instanceof Number)
			return ((Number) val).intValue();
		try
		{
			return Integer.parseInt((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}

	public short getShort(String name)
	{
		Object val = _set.get(name);
		if(val == null)
			throw new IllegalArgumentException("Integer value required, but not specified");
		if(val instanceof Number)
			return ((Number) val).shortValue();
		try
		{
			return Short.parseShort((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}

	public Short getShort(String name, Short deflt)
	{
		Object val = _set.get(name);
		if(val == null)
			return deflt;
		if(val instanceof Number)
			return ((Number) val).shortValue();
		try
		{
			return Short.parseShort((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}

	public byte getByte(String name)
	{
		Object val = _set.get(name);
		if(val == null)
			throw new IllegalArgumentException("Integer value required, but not specified");
		if(val instanceof Number)
			return ((Number) val).byteValue();
		try
		{
			return Byte.parseByte((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}

	public Byte getByte(String name, Byte deflt)
	{
		Object val = _set.get(name);
		if(val == null)
			return deflt;
		if(val instanceof Number)
			return ((Number) val).byteValue();
		try
		{
			return Byte.parseByte((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}

	/**
	 * Returns the int associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : int designating the default value if value associated with the key is null
	 * @return int : value associated to the key
	 */
	public int getInteger(String name, int deflt)
	{
		Object val = _set.get(name);
		if(val == null)
			return deflt;
		if(val instanceof Number)
			return ((Number) val).intValue();
		try
		{
			return Integer.parseInt((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}

	/**
	 * Returns the float associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return float : value associated to the key
	 */
	public float getFloat(String name)
	{
		Object val = _set.get(name);
		if(val == null)
			throw new IllegalArgumentException("Float value required, but not specified");
		if(val instanceof Number)
			return ((Number) val).floatValue();
		try
		{
			return (float) Double.parseDouble((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}

	/**
	 * Returns the float associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : float designating the default value if value associated with the key is null
	 * @return float : value associated to the key
	 */
	public float getFloat(String name, float deflt)
	{
		Object val = _set.get(name);
		if(val == null)
			return deflt;
		if(val instanceof Number)
			return ((Number) val).floatValue();
		try
		{
			return (float) Double.parseDouble((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}

	/**
	 * Returns the double associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return double : value associated to the key
	 */
	public double getDouble(String name)
	{
		Object val = _set.get(name);
		if(val == null)
			throw new IllegalArgumentException("Float value required, but not specified");
		if(val instanceof Number)
			return ((Number) val).doubleValue();
		try
		{
			return Double.parseDouble((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}

	/**
	 * Returns the double associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : float designating the default value if value associated with the key is null
	 * @return double : value associated to the key
	 */
	public double getDouble(String name, double deflt, String effName)
	{
		System.out.println("getDouble for effect name: " + effName);
		return getDouble(name, deflt);
	}

	public double getDouble(String name, double deflt)
	{
		Object val = _set.get(name);
		if(val == null)
			return deflt;
		if(val instanceof Number)
			return ((Number) val).doubleValue();
		try
		{
			return Double.parseDouble((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}

	/**
	 * Returns the String associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return String : value associated to the key
	 */
	public String getString(String name)
	{
		Object val = _set.get(name);
		if(val == null)
			throw new IllegalArgumentException("String value required, but not specified");
		return String.valueOf(val);
	}

	/**
	 * Returns the String associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : String designating the default value if value associated with the key is null
	 * @return String : value associated to the key
	 */
	public String getString(String name, String deflt)
	{
		Object val = _set.get(name);
		if(val == null)
			return deflt;
		return String.valueOf(val);
	}

	/**
	 * Returns the double associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return double : value associated to the key
	 */
	public long getLong(String name)
	{
		Object val = _set.get(name);
		if(val == null)
			throw new IllegalArgumentException("Integer value required, but not specified");
		if(val instanceof Number)
			return ((Number) val).longValue();
		try
		{
			return Long.parseLong((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}

	/**
	 * Returns the Long associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : Long designating the default value if value associated with the key is null
	 * @return Long : value associated to the key
	 */
	public long getLong(String name, long deflt)
	{
		Object val = _set.get(name);
		if(val == null)
			return deflt;
		if(val instanceof Number)
			return ((Number) val).longValue();
		try
		{
			return Long.parseLong((String) val);
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}

	/**
	 * Returns an enumeration of &lt;T&gt; from the set
	 * @param <T> : Class of the enumeration returned
	 * @param name : String designating the key in the set
	 * @param enumClass : Class designating the class of the value associated with the key in the set
	 * @return Enum<T>
	 */
	@SuppressWarnings(value = { "unchecked" })
	public <T extends Enum<T>> T getEnum(String name, Class<T> enumClass)
	{
		Object val = _set.get(name);
		if(val == null)
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but not specified");
		if(enumClass.isInstance(val))
			return (T) val;
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val));
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + "required, but found: " + val);
		}
	}

	/**
	 * Returns an enumeration of &lt;T&gt; from the set. If the enumeration is empty, the method returns the value of the parameter "deflt".
	 * @param <T> : Class of the enumeration returned
	 * @param name : String designating the key in the set
	 * @param enumClass : Class designating the class of the value associated with the key in the set
	 * @param deflt : <T> designating the value by default
	 * @return Enum<T>
	 */
	@SuppressWarnings(value = { "unchecked" })
	public <T extends Enum<T>> T getEnum(String name, Class<T> enumClass, T deflt)
	{
		Object val = _set.get(name);
		if(val == null)
			return deflt;
		if(enumClass.isInstance(val))
			return (T) val;
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val));
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + "required, but found: " + val);
		}
	}

	public L2Skill getSkill(String name)
	{
		return getSkill(name, null);
	}

	public L2Skill getSkill(String name, String def)
	{
		Object val = _set.get(name);
		if(val == null && def != null)
			return SkillTable.parseSkillInfo(def);

		if(val instanceof String)
			return SkillTable.parseSkillInfo((String) val);

		return null;
	}

	/**
	 * Add the String hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : String corresponding to the value associated with the key
	 */
	public void set(String name, String value)
	{
		_set.put(name, value);
	}

	/**
	 * Add the boolean hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : boolean corresponding to the value associated with the key
	 */
	public void set(String name, boolean value)
	{
		_set.put(name, value);
	}

	/**
	 * Add the int hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : int corresponding to the value associated with the key
	 */
	public void set(String name, int value)
	{
		_set.put(name, value);
	}

	public void set(String name, Object value)
	{
		_set.put(name, value);
	}

	/**
	 * Add the double hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : double corresponding to the value associated with the key
	 */
	public void set(String name, double value)
	{
		_set.put(name, value);
	}

	/**
	 * Add the Enum hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : Enum corresponding to the value associated with the key
	 */
	public void set(String name, Enum<?> value)
	{
		_set.put(name, value);
	}

	/**
	 * Add the long hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : long corresponding to the value associated with the key
	 */
	public void set(String name, long value)
	{
		_set.put(name, value);
	}

	@Override
	public StatsSet clone()
	{
		return new StatsSet(_set);
	}

	public void unset(String name)
	{
		_set.remove(name);
	}
}
