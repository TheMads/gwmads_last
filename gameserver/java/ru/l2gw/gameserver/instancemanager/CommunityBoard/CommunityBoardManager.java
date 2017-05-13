package ru.l2gw.gameserver.instancemanager.CommunityBoard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.templates.StatsSet;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rage
 * @date 25.02.2010 17:21:24
 */
public class CommunityBoardManager
{
	private static CommunityBoardManager _instance;
	private static Map<String, ICommunityBoardHandler> _handlers;
	private static Log _log = LogFactory.getLog(CommunityBoardManager.class.getSimpleName());
	private static final StatsSet _properties = new StatsSet();

	private CommunityBoardManager()
	{
		_handlers = new HashMap<>();
	}

	public static CommunityBoardManager getInstance()
	{
		if(_instance == null)
			_instance = new CommunityBoardManager();
		return _instance;
	}

	public void registerHandler(ICommunityBoardHandler commHandler)
	{
		for(String bypass : commHandler.getBypassCommands())
		{
			if(_handlers.containsKey(bypass))
				_log.warn("CommunityBoard: dublicate bypass registered! First handler: " + _handlers.get(bypass).getClass().getSimpleName() + " second: " + commHandler.getClass().getSimpleName());

			_handlers.put(bypass, commHandler);
		}
	}

	public void unregisterHandler(ICommunityBoardHandler handler)
	{
		for(String bypass : handler.getBypassCommands())
			_handlers.remove(bypass);
		_log.info("CommunityBoard: " + handler.getClass().getSimpleName() + " unloaded.");
	}

	public ICommunityBoardHandler getCommunityHandler(String bypass)
	{
		if(!Config.COMMUNITYBOARD_ENABLED || _handlers.size() < 1)
			return null;

		for(String handler : _handlers.keySet())
			if(bypass.startsWith(handler))
				return _handlers.get(handler);

		return null;
	}

	public void setProperty(String name, String val)
	{
		_properties.set(name, val);
	}

	public void setProperty(String name, int val)
	{
		_properties.set(name, val);
	}

	public int getIntProperty(String name)
	{
		return _properties.getInteger(name, 0);
	}
}
