package ru.l2gw.gameserver.taskmanager;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class ItemsAutoDestroy
{
	private static ItemsAutoDestroy _instance;
	ConcurrentLinkedQueue<L2ItemInstance> _items = null;
	ConcurrentLinkedQueue<L2ItemInstance> _herbs = null;

	private ItemsAutoDestroy()
	{
		_items = new ConcurrentLinkedQueue<L2ItemInstance>();
		_herbs = new ConcurrentLinkedQueue<L2ItemInstance>();
		if(Config.AUTODESTROY_ITEM_AFTER > 0)
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckItemsForDestroy(), 60000, 60000);
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckHerbsForDestroy(), 1000, 1000);
	}

	public static ItemsAutoDestroy getInstance()
	{
		if(_instance == null)
			_instance = new ItemsAutoDestroy();
		return _instance;
	}

	public void addItem(L2ItemInstance item)
	{
		item.setDropTime(System.currentTimeMillis());
		_items.add(item);
	}

	public void addHerb(L2ItemInstance herb)
	{
		herb.setDropTime(System.currentTimeMillis());
		_herbs.add(herb);
	}

	public ConcurrentLinkedQueue<L2ItemInstance> getKnownItems()
	{
		return _items;
	}

	public class CheckItemsForDestroy extends Thread
	{
		@Override
		public void run()
		{
			try
			{
				long curtime = System.currentTimeMillis();
				for(L2ItemInstance item : _items)
					if(item == null || item.getDropTime() == 0 || item.getLocation() != L2ItemInstance.ItemLocation.VOID)
						_items.remove(item);
					else if(item.getDropTime() + Config.AUTODESTROY_ITEM_AFTER < curtime)
					{
						item.decayMe();
						L2World.removeObject(item);
						_items.remove(item);
					}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public class CheckHerbsForDestroy extends Thread
	{
		@Override
		public void run()
		{
			try
			{
				long curtime = System.currentTimeMillis();
				for(L2ItemInstance item : _herbs)
					if(item == null || item.getDropTime() == 0 || item.getLocation() != L2ItemInstance.ItemLocation.VOID)
						_herbs.remove(item);
					else if(item.getDropTime() + Config.ALT_HERB_LIFE_TIME < curtime)
					{
						item.decayMe();
						L2World.removeObject(item);
						_herbs.remove(item);
					}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}