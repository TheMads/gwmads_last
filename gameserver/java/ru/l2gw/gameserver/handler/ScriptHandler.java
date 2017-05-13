package ru.l2gw.gameserver.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

import java.util.LinkedList;
import java.util.List;

public class ScriptHandler implements IOnDieHandler, IOnEscapeHandler, IOnResurrectHandler
{
	private static final Log log = LogFactory.getLog(ScriptHandler.class);
	private static ScriptHandler instance;
	private final List<IOnDieHandler> onDieHandlers;
	private final List<IOnResurrectHandler> onResurrectHandlers;
	private final List<IOnEscapeHandler> onEscapeHandlers;

	public ScriptHandler()
	{
		onDieHandlers = new LinkedList<>();
		onResurrectHandlers = new LinkedList<>();
		onEscapeHandlers = new LinkedList<>();
	}

	public static ScriptHandler getInstance()
	{
		if(instance == null)
			instance = new ScriptHandler();
		return instance;
	}

	public void registerOnDieHandler(IOnDieHandler handler)
	{
		if(!onDieHandlers.contains(handler))
			onDieHandlers.add(handler);
	}

	public void registerOnResurrectHandler(IOnResurrectHandler handler)
	{
		if(!onResurrectHandlers.contains(handler))
			onResurrectHandlers.add(handler);
	}

	public void registerOnEscapeHandler(IOnEscapeHandler handler)
	{
		if(!onEscapeHandlers.contains(handler))
			onEscapeHandlers.add(handler);
	}

	@Override
	public void onDie(L2Character self, L2Character killer)
	{
		for(IOnDieHandler handler : onDieHandlers)
		{
			try
			{
				handler.onDie(self, killer);
			}
			catch(Throwable t)
			{
				 log.error("ScriptHandler: onDie error in class: " + handler.getClass().getCanonicalName() + " " + t, t);
			}
		}
	}

	@Override
	public Location onEscape(L2Player player)
	{
		for(IOnEscapeHandler handler : onEscapeHandlers)
		{
			try
			{
				Location loc = handler.onEscape(player);
				if(loc != null)
					return loc;
			}
			catch(Throwable t)
			{
				log.error("ScriptHandler: onEscape error in class: " + handler.getClass().getCanonicalName() + " " + t, t);
			}
		}

		return null;
	}

	@Override
	public void onResurrected(L2Player player, long reviverStoredId)
	{
		for(IOnResurrectHandler handler : onResurrectHandlers)
		{
			try
			{
				handler.onResurrected(player, reviverStoredId);
			}
			catch(Throwable t)
			{
				log.error("ScriptHandler: onResurrected error in class: " + handler.getClass().getCanonicalName() + " " + t, t);
			}
		}
	}
}
