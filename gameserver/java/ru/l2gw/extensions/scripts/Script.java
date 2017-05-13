package ru.l2gw.extensions.scripts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;

public class Script implements Comparable<Script>
{
	private static final Log _log = LogFactory.getLog(Script.class.getName());
	private Class<?> _class;

	public Script(Class<?> c)
	{
		_class = c;
	}

	public ScriptObject newInstance()
	{
		ScriptObject o = null;
		Object instance = null;
		try
		{
			instance = _class.newInstance();
		}
		catch(InstantiationException e)
		{
			if(Config.DEBUG)
				_log.info("Class " + getName() + " hasn't default constructor.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		o = new ScriptObject(_class, instance);

		return o;
	}

	@SuppressWarnings("unchecked")
	public ScriptObject newInstance(Object[] args)
	{
		ScriptObject o = null;
		Object instance = null;
		try
		{
			Class<?>[] types = new Class<?>[args.length];
			boolean arg = false;
			for(int i = 0; i < args.length; i++)
				if(args[i] != null)
				{
					types[i] = args[i].getClass();
					arg = true;
				}
			if(!arg)
				return newInstance();
			instance = _class.getConstructor(types).newInstance(args);
		}
		catch(InstantiationException e)
		{
			if(Config.DEBUG)
				_log.info("Class " + getName() + " hasn't constructor with such arguments.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		o = new ScriptObject(_class, instance);

		return o;
	}

	public Class<?> getRawClass()
	{
		return _class;
	}

	public String getName()
	{
		return _class.getName();
	}

	@Override
	public int compareTo(Script o)
	{
		return getName().compareTo(o.getName());
	}
}
