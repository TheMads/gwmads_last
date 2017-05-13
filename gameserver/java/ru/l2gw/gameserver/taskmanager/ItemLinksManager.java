package ru.l2gw.gameserver.taskmanager;

import javolution.util.FastMap;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author rage
 * @date 15.01.2010 17:18:47
 */
public class ItemLinksManager
{
	private static FastMap<Integer, ItemLink> _itemLinks;
	private static ItemLinksManager _instance;
	private static final Pattern _pattern = Pattern.compile("(Type=\\d+ \tID=(\\d+) \tColor=\\d+ \tUnderline=\\d+ \tTitle=)");
	//private static final Pattern _pattern = Pattern.compile("(Type=\\d+ ID=(\\d+) Color=\\d+ Underline=\\d+ Title=)");
	private static ScheduledFuture<?> _clearTask;

	public static ItemLinksManager getInstance()
	{
		if(_instance == null)
			_instance = new ItemLinksManager();

		return _instance;
	}

	private ItemLinksManager()
	{
		_itemLinks = new FastMap<Integer, ItemLink>().shared();
		_clearTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ClearTask(), 60000, 60000);
	}

	public void addItemLinks(L2Player player, String message)
	{
		Matcher m = _pattern.matcher(message);
		while(m.find())
		{
			try
			{
				int objectId = Integer.parseInt(m.group(2));
				L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
				if(item != null)
					addItemToCache(item);
			}
			catch(Exception e)
			{
			}
		}
	}

	private void addItemToCache(L2ItemInstance item)
	{
		if(_itemLinks.containsKey(item.getObjectId()))
			_itemLinks.remove(item.getObjectId());

		_itemLinks.put(item.getObjectId(), new ItemLink(item));
	}

	public L2ItemInstance getItem(int objectId)
	{
		ItemLink il = _itemLinks.get(objectId);
		if(il == null || il.item == null || il.item.getCount() < 1)
		{
			if(il != null)
				_itemLinks.remove(objectId);
			return null;
		}

		return il.item;
	}

	private class ClearTask implements Runnable
	{
		public void run()
		{
			for(ItemLink il : _itemLinks.values())
				if(il.expireTime < System.currentTimeMillis() || il.item == null || il.item.getCount() < 1)
					_itemLinks.remove(il.objectId);
		}
	}

	private class ItemLink
	{
		public final L2ItemInstance item;
		public final long expireTime;
		public final int objectId;

		public ItemLink(final L2ItemInstance item)
		{
			this.item = item;
			objectId = item.getObjectId();
			expireTime = System.currentTimeMillis() + Config.ITEM_LINK_SHOW_TIME;
		}
	}
}
