package ru.l2gw.gameserver.model;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

import java.util.List;

public class ClanWarehousePool
{
	private class ClanWarehouseWork
	{
		private L2Player player;
		private L2ItemInstance[] items;
		private long[] counts;
		public boolean complete;

		public ClanWarehouseWork(L2Player _player, L2ItemInstance[] _items, long[] _counts)
		{
			player = _player;
			items = _items;
			counts = _counts;
			complete = false;
		}

		public synchronized void RunWork()
		{
			Warehouse warehouse2 = player.getClan().getWarehouse();

			for(int i = 0; i < items.length; i++)
			{
				if(counts[i] < 0)
				{
					_log.warn("Warning char:" + player.getName() + " get Item from ClanWarhouse count < 0: objid=" + items[i].getObjectId());
					return;
				}
				warehouse2.transferItem("CWhOut", items[i].getObjectId(), counts[i], player.getInventory(), player, player.getLastNpc());
			}

			player.sendChanges();

			complete = true;
		}
	}

	static final Log _log = LogFactory.getLog(ClanWarehousePool.class.getName());

	private static ClanWarehousePool _instance;
	private List<ClanWarehouseWork> _works;
	private boolean inWork;

	public static ClanWarehousePool getInstance()
	{
		if(_instance == null)
			_instance = new ClanWarehousePool();
		return _instance;
	}

	public ClanWarehousePool()
	{
		_works = new FastList<ClanWarehouseWork>();
		inWork = false;
	}

	public void AddWork(L2Player _player, L2ItemInstance[] _items, long[] _counts)
	{
		ClanWarehouseWork cww = new ClanWarehouseWork(_player, _items, _counts);
		_works.add(cww);
		if(Config.DEBUG)
			_log.warn("ClanWarehousePool: add work, work count " + _works.size());
		RunWorks();
	}

	private void RunWorks()
	{
		if(inWork)
		{
			if(Config.DEBUG)
				_log.warn("ClanWarehousePool: work in progress, work count " + _works.size());
			return;
		}

		inWork = true;
		try
		{
			if(_works.size() > 0)
			{
				ClanWarehouseWork cww = _works.get(0);
				if(!cww.complete)
				{
					if(Config.DEBUG)
						_log.warn("ClanWarehousePool: run work, work count " + _works.size());
					cww.RunWork();
				}
				_works.remove(0);
			}
		}
		catch(Exception e)
		{
			_log.warn("Error ClanWarehousePool: " + e);
		}
		inWork = false;

		if(_works.size() > 0)
			RunWorks();
	}
}