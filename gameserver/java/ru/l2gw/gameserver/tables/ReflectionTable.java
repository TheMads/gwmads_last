package ru.l2gw.gameserver.tables;

import ru.l2gw.gameserver.model.Reflection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionTable
{
	private static ReflectionTable _instance;

	public static ReflectionTable getInstance()
	{
		if(_instance == null)
			_instance = new ReflectionTable();
		return _instance;
	}

	private Map<Integer, Reflection> _list = new ConcurrentHashMap<>();

	public void addReflection(Reflection r)
	{
		_list.put(r.getId(), r);
	}

	public Reflection removeReflection(int id)
	{
		return _list.remove(id);
	}

	public Reflection getById(int id, boolean CreateIfNonExist)
	{
		Reflection re = _list.get(id);
		if(CreateIfNonExist && re == null)
		{
			re = new Reflection(id);
			addReflection(re);
		}
		return re;
	}

	public Reflection getById(int id)
	{
		return _list.get(id);
	}

	public synchronized int createNewReflection()
	{
		int id = 1;
		while(_list.containsKey(id))
			id++;

		_list.put(id, new Reflection(id));

		return id;
	}
}