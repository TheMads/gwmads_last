package ru.l2gw.gameserver.model.entity.itemauction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ItemAuctionManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.playerSubOrders.UserVar;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * @author: rage
 * @date: 28.08.2010 13:42:20
 */
public class ItemAuction
{
	private static final Log _log = LogFactory.getLog("itemauction");

	private final ItemAuctionTemplate _template;
	private int _auctionId;
	private final int _itemId;
	private final AuctionItem _item;
	private long _startDate;
	private long _endDate;
	private long _currentBid;
	private int _currentBidder;
	private final AuctionItem _prevItem;
	private long _prevBid;
	private boolean _finished;
	private AuctionItem _nextItem;
	private long _nextDate;
	private int _extendedTime = 0;

	private ScheduledFuture<?> _endTask;
	private ScheduledFuture<?> _startTask;

	public ItemAuction(ItemAuctionTemplate template, int auctionId, int itemId, long currentBid, int currentBidder, long startDate, long endDate, int prevItemId, long prevBid, boolean finished)
	{
		_template = template;
		_auctionId = auctionId;
		_itemId = itemId;
		_currentBid = currentBid;
		_currentBidder = currentBidder;
		_startDate = startDate;
		_endDate = endDate;
		_prevBid = prevBid;
		_finished = finished;
		_item = _template.getAuctionItemById(_itemId);
		_prevItem = _template.getAuctionItemById(prevItemId);
	}

	public static ItemAuction createAuction(ItemAuctionTemplate template)
	{
		Calendar start = Calendar.getInstance();
		start.set(Calendar.DAY_OF_WEEK, template.getStartDay());
		start.set(Calendar.HOUR_OF_DAY, template.getStartHour());
		start.set(Calendar.MINUTE, template.getStartMin());
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		while(start.getTimeInMillis() < System.currentTimeMillis())
			start.add(Calendar.DAY_OF_MONTH, 7);

		AuctionItem item = template.getRandomItem();
		AuctionItem prevItem = template.getRandomItem();
		long startDate = start.getTimeInMillis();
		ItemAuction ia = new ItemAuction(template, 0, item.getItemId(), item.getStartBid(), 0, startDate, startDate + template.getAuctionTime(), prevItem.getItemId(), prevItem.getStartBid(), false);
		ia.store();
		return ia;
	}

	public void store()
	{
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			if(_auctionId == 0)
			{
				stmt = con.prepareStatement("INSERT INTO `item_auction`(`broker_id`, `item_id`, `current_bid`, `bidder_id`, `start_date`, `end_date`, `finished`, `prev_item_id`, `prev_bid`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
				stmt.setInt(1, _template.getBrokerId());
				stmt.setInt(2, _itemId);
				stmt.setLong(3, _currentBid);
				stmt.setInt(4, _currentBidder);
				stmt.setInt(5, (int) (_startDate / 1000));
				stmt.setInt(6, (int) (_endDate / 1000));
				stmt.setBoolean(7, _finished);
				stmt.setInt(8, _prevItem.getItemId());
				stmt.setLong(9, _prevBid);
				stmt.execute();

				DbUtils.closeQuietly(stmt);
				stmt = con.prepareStatement("SELECT LAST_INSERT_ID()");
				rs = stmt.executeQuery();

				if(rs.next())
					_auctionId = rs.getInt(1);
			}
			else
			{
				stmt = con.prepareStatement("REPLACE INTO `item_auction`(`auction_id`, `broker_id`, `item_id`, `current_bid`, `bidder_id`, `start_date`, `end_date`, `finished`, `prev_item_id`, `prev_bid`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				stmt.setInt(1, _auctionId);
				stmt.setInt(2, _template.getBrokerId());
				stmt.setInt(3, _itemId);
				stmt.setLong(4, _currentBid);
				stmt.setInt(5, _currentBidder);
				stmt.setInt(6, (int) (_startDate / 1000));
				stmt.setInt(7, (int) (_endDate / 1000));
				stmt.setBoolean(8, _finished);
				stmt.setInt(9, _prevItem.getItemId());
				stmt.setLong(10, _prevBid);
				stmt.execute();
			}
		}
		catch(Exception e)
		{
			_log.warn(this + " can't store item aution data: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt, rs);
		}
	}

	public void startAuction()
	{
		if(_endDate < System.currentTimeMillis()) // Auction ended
			endAuction();
		else if(_startDate > System.currentTimeMillis())
			scheduleStartTask();
		else
			scheduleEndTask();
	}

	private ItemAuction createNextAuction()
	{
		Calendar start = Calendar.getInstance();
		start.set(Calendar.DAY_OF_WEEK, _template.getStartDay());
		start.set(Calendar.HOUR_OF_DAY, _template.getStartHour());
		start.set(Calendar.MINUTE, _template.getStartMin());
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		while(start.getTimeInMillis() < System.currentTimeMillis())
			start.add(Calendar.DAY_OF_MONTH, 7);

		AuctionItem item = _nextItem == null ? _template.getRandomItem() : _nextItem;
		return new ItemAuction(_template, 0, item.getItemId(), item.getStartBid(), 0, start.getTimeInMillis(), start.getTimeInMillis() + _template.getAuctionTime(), _itemId, _currentBid, false);
	}

	private void scheduleStartTask()
	{
		_log.info(this + " start date: " + new Date(_startDate));

		if(_startTask != null)
			_startTask.cancel(true);

		_startTask = ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _startDate - System.currentTimeMillis());
	}

	private void scheduleEndTask()
	{
		_log.info(this + " end date: " + new Date(_endDate));
		if(_endTask != null)
			_endTask.cancel(true);

		if(_nextItem == null)
		{
			_nextItem = _template.getRandomItem();
			_nextDate = _endDate + 7 * 24 * 60 * 60000L;
		}

		_endTask = ThreadPoolManager.getInstance().scheduleGeneral(new EndTask(), _endDate - System.currentTimeMillis());
	}

	private void endAuction()
	{
		if(!_finished)
		{
			_finished = true;
			if(_currentBidder > 0)
				giveItem();
			store();
		}

		ItemAuction ia = createNextAuction();
		ia.store();
		ItemAuctionManager.getInstance().addAuction(ia);
		ia.startAuction();
	}

	public synchronized SystemMessage setBid(L2Player player, long bid)
	{
		if(_finished)
			return Msg.IT_IS_NOT_AN_AUCTION_PERIOD;

		if(bid <= _currentBid)
			return Msg.YOUR_BID_MUST_BE_HIGHER_THAN_THE_CURRENT_HIGHEST_BID;

		if(player.reduceAdena("BidItemAuction", bid, L2ObjectsStorage.getByNpcId(getBrokerId()), true))
		{
			L2Player oldBidder = null;

			if(_currentBidder > 0)
			{
				oldBidder = L2ObjectsStorage.getPlayer(_currentBidder);
				if(oldBidder != null)
				{
					oldBidder.sendPacket(Msg.YOU_HAVE_BEEN_OUTBID);
					long old = 0;
					try
					{
						old = Long.parseLong(oldBidder.getVar("bid-" + getBrokerId()));
					}
					catch(Exception e)
					{
					}
					oldBidder.setVar("bid-" + getBrokerId(), String.valueOf(old + _currentBid));
				}
				else
				{
					Map<String, UserVar> vars = L2Player.loadVariables(_currentBidder);
					UserVar uv = vars.get("bid-" + getBrokerId());
					if(uv != null)
						uv.value = String.valueOf(_currentBid + Long.parseLong(uv.value));
					else
						uv = new UserVar("bid-" + getBrokerId(), String.valueOf(_currentBid), _nextDate - 600000);
					L2Player.saveUserVar(_currentBidder, uv);
				}
			}

			_currentBid = bid;
			_currentBidder = player.getObjectId();

			if(getTimeLeft() < 600 && _extendedTime == 0)
			{
				_extendedTime = 1;
				_endDate += 300000;
				scheduleEndTask();
				player.sendPacket(Msg.BIDDER_EXISTS_THE_AUCTION_TIME_HAS_BEEN_EXTENDED_BY_5_MINUTES);
				if(oldBidder != null)
					oldBidder.sendPacket(Msg.BIDDER_EXISTS_THE_AUCTION_TIME_HAS_BEEN_EXTENDED_BY_5_MINUTES);
			}
			else if(getTimeLeft() < 300)
			{
				_extendedTime++;
				if(_extendedTime == 4)
				{
					_endDate += 180000;
					scheduleEndTask();
					player.sendPacket(Msg.BIDDER_EXISTS_AUCTION_TIME_HAS_BEEN_EXTENDED_BY_3_MINUTES);
					if(oldBidder != null)
						oldBidder.sendPacket(Msg.BIDDER_EXISTS_AUCTION_TIME_HAS_BEEN_EXTENDED_BY_3_MINUTES);
				}
			}

			store();
			return Msg.YOU_HAVE_BID_ON_AN_ITEM_AUCTION;
		}

		return Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA_FOR_THIS_BID;
	}

	public int getCurrentBidderId()
	{
		return _currentBidder;
	}

	public boolean isStarted()
	{
		return _startDate < System.currentTimeMillis();
	}

	public int getAuctionId()
	{
		return _auctionId;
	}

	public int getTimeLeft()
	{
		return (int) ((_endDate - System.currentTimeMillis()) / 1000);
	}

	public AuctionItem getItem()
	{
		return _item;
	}

	public long getCurrentBid()
	{
		return _currentBid;
	}

	public AuctionItem getPrevItem()
	{
		return _prevItem;
	}

	public AuctionItem getNextItem()
	{
		return _nextItem;
	}

	public long getPrevBid()
	{
		return _prevBid;
	}

	public int getStartDate()
	{
		return (int) (_startDate / 1000); 
	}

	public int getNextDate()
	{
		return (int) (_nextDate / 1000);
	}

	public int getBrokerId()
	{
		return _template.getBrokerId();
	}

	private void giveItem()
	{
		L2NpcInstance broker = L2ObjectsStorage.getByNpcId(_template.getBrokerId());
		L2ItemInstance item = ItemTable.getInstance().createItem("ItemAuction", _itemId, 1, broker);
		AuctionItem ai = _template.getAuctionItemById(_itemId);

		if(ai.getEnchantLevel() > 0)
			item.setEnchantLevel(ai.getEnchantLevel());

		if(ai.getCount() > 1 && item.isStackable())
			item.setCount(ai.getCount());

		L2Player player = L2ObjectsStorage.getPlayer(_currentBidder);
		if(player != null)
		{
			//player.unsetVar("bid-" + _template.getBrokerId());
			player.getWarehouse().addItem("ItemAuction", item, player, broker);
			player.sendPacket(Msg.YOU_HAVE_BID_THE_HIGHEST_PRICE_AND_HAVE_WON_THE_ITEM_THE_ITEM_CAN_BE_FOUND_IN_YOUR_PERSONAL_WAREHOUSE);
		}
		else
		{
			//L2Player.unsetVar(_currentBidder, "bid-" + _template.getBrokerId());
			item.setOwnerId(_currentBidder);
			item.changeLocation("ItemAuction", L2ItemInstance.ItemLocation.WAREHOUSE, null, broker);
			item.updateDatabase(true);
		}
	}

	private class StartTask implements Runnable
	{
		public void run()
		{
			scheduleEndTask();
			for(L2Player player : L2ObjectsStorage.getAllPlayers())
				player.sendPacket(Msg.THE_AUCTION_HAS_BEGUN);
		}
	}

	private class EndTask implements Runnable
	{
		public void run()
		{
			endAuction();
		}
	}

	@Override
	public String toString()
	{
		return "ItemAuction[brokerId=" + getBrokerId() + ";id=" + _auctionId + ";itemId=" + _itemId + "]:";
	}
}
