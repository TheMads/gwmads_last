package ru.l2gw.gameserver.model.entity;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.AuctionManager;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

public class Auction implements Comparable<Auction>
{
	private static Log _log = LogFactory.getLog("clanhall");

	private final int _clanHallId;
	private final int _clanId;
	private final long _deposit;
	private final long _startBid;
	private final Calendar _endDate;
	private final String _description;
	private final FastMap<Integer, Bidder> _bidders;
	private ScheduledFuture<?> _endTask = null;
	private static SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy H:m:s");

	public Auction(int clanHallId, int clanId, long deposit, long startingBid, long endDate, String descr)
	{
		_clanHallId = clanHallId;
		_clanId = clanId;
		_startBid = startingBid;
		_deposit = deposit;
		_endDate = Calendar.getInstance();
		_endDate.setTimeInMillis(endDate);
		_description = descr != null ? descr : "";
		_bidders = new FastMap<Integer, Bidder>().shared();
		if(endDate > System.currentTimeMillis())
			_endTask = ThreadPoolManager.getInstance().scheduleGeneral(new EndTask(), endDate - System.currentTimeMillis());
	}

	public String getClanName()
	{
		if(_clanId > 0)
		{
			L2Clan clan = ClanTable.getInstance().getClan(_clanId);
			if(clan == null)
				return "NPC Clan";

			return clan.getName();
		}

		return "NPC Clan";
	}

	public String getClanLeaderName()
	{
		if(_clanId > 0)
		{
			L2Clan clan = ClanTable.getInstance().getClan(_clanId);
			if(clan == null)
			{
				_log.warn(this + ": clanId: " + _clanId + " is null!");
				return "NPC";
			}
			return clan.getLeaderName();
		}

		return "NPC";
	}

	public Calendar getEndDate()
	{
		return _endDate;
	}

	public long getStartBid()
	{
		return _startBid;
	}

	public long getDeposit()
	{
		return _deposit;
	}

	public int getClanHallId()
	{
		return _clanHallId;
	}

	public L2Clan getClan()
	{
		if(_clanId > 0)
			return ClanTable.getInstance().getClan(_clanId);
		return null;
	}

	public ClanHall getClanHall()
	{
		return (ClanHall) ResidenceManager.getInstance().getBuildingById(_clanHallId);
	}

	public String getDescription()
	{
		if(_clanId > 0)
			return _description;

		return getClanHall().getDesc();
	}

	public GArray<Bidder> getBidders()
	{
		GArray<Bidder> ret = new GArray<Bidder>();
		ret.addAll(_bidders.values());
		return ret;
	}

	public boolean isBidder(int clanId)
	{
		return _bidders.containsKey(clanId);
	}

	public Bidder getBidder(int clanId)
	{
		return _bidders.get(clanId);
	}

	public void loadBidders()
	{
		_bidders.clear();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM agit_auction_bid WHERE agit_id = ? ORDER BY bid_time");
			statement.setInt(1, _clanHallId);
			rs = statement.executeQuery();

			while(rs.next())
				_bidders.put(rs.getInt("clan_id"), new Bidder(rs.getInt("clan_id"), rs.getLong("bid"), rs.getLong("bid_time")));
		}
		catch(Exception e)
		{
			_log.warn(this + ": Can't load bidders: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	public void addBidder(L2Clan clan, long bid)
	{
		Bidder b = new Bidder(clan.getClanId(), bid, System.currentTimeMillis());
		b.store();
		clan.setAuctionBiddedAt(_clanHallId, 0);
		_bidders.put(clan.getClanId(), b);
		L2Clan owner = getClan();
		if(owner != null)
			owner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.YOU_HAVE_BID_IN_A_CLAN_HALL_AUCTION));
	}

	public void removeBidder(int clanId)
	{
		Bidder b = _bidders.remove(clanId);
		if(b != null)
			b.deleteMe();
	}

	public void store()
	{
		Connection con = null;
		PreparedStatement statement = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO agit_auction VALUES(?, ?, ?, ?, ?, ?)");
			statement.setInt(1, _clanHallId);
			statement.setInt(2, _clanId);
			statement.setLong(3, _startBid);
			statement.setLong(4, _deposit);
			statement.setString(5, _description);
			statement.setLong(6, _endDate.getTimeInMillis());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn(this + ": Can't store: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void deleteMe()
	{
		Connection con = null;
		PreparedStatement statement = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM agit_auction_bid WHERE agit_id = ?");
			statement.setInt(1, _clanHallId);
			statement.execute();

			statement = con.prepareStatement("DELETE FROM agit_auction WHERE agit_id = ?");
			statement.setInt(1, _clanHallId);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn(this + ": Can't delete" + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);

			if(_endTask != null)
			{
				_endTask.cancel(true);
				_endTask = null;
			}

			for(Bidder b : _bidders.values())
			{
				L2Clan clan = ClanTable.getInstance().getClan(b.getClanId());
				if(clan != null)
				{
					clan.setAuctionBiddedAt(0, 0);
					clan.getWarehouse().addItem("RemoveBid", 57, (long) (b.getBid() * 0.90), null, null);
					_log.info(this + ": remove bidder: " + b.getClanName() + " return bid: " + (long) (b.getBid() * 0.90));
				}
				b.deleteMe();
			}
			_bidders.clear();
		}
	}

	public boolean isEndTaskStarted()
	{
		return _endTask != null;
	}

	public long getMaxBid()
	{
		if(_bidders.size() > 0)
		{
			FastList<Bidder> bidders = new FastList<Bidder>(_bidders.values());
			Collections.sort(bidders);
			return bidders.getFirst().getBid();
		}

		return _startBid;
	}

	public void endAuction()
	{
		_log.info(this + ": end, bidders count: " + _bidders.size());
		L2Clan clan = getClan();

		if(_bidders.size() > 0)
		{
			FastList<Bidder> bidders = new FastList<Bidder>(_bidders.values());
			Collections.sort(bidders);
			int s = bidders.size();

			_log.info(this + ": bidders list:");
			for(Bidder b : bidders)
				_log.info(this + ": " + b);

			Bidder winner = null;
			L2Clan winClan = null;

			while(s > 0)
			{
				winner = bidders.removeFirst();
				winClan = ClanTable.getInstance().getClan(winner.getClanId());
				if(winClan != null)
					break;
				s--;
			}

			if(winClan == null)
			{
				if(clan == null)
				{
					_endTask = null;
					_log.info(this + ": end, no winner clan WTF? start auction again.");
					AuctionManager.getInstance().addToAuction(getClanHall());
				}
				else
				{
					_endTask = null;
					_log.info(this + ": end, no winner clan WTF? stop auction.");
					AuctionManager.getInstance().removeAuction(getClanHall());
					clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.THE_CLAN_HALL_WHICH_HAD_BEEN_PUT_UP_FOR_AUCTION_WAS_NOT_SOLD_AND_THEREFORE_HAS_BEEN_RELISTED));
				}
			}
			else
			{
				_endTask = null;
				getClanHall().setLastPrice(winner.getBid());
				getClanHall().updateLastPrice();
				_log.info(this + ": end, winner: " + winner);
				_bidders.remove(winner.getClanId());
				AuctionManager.getInstance().removeAuction(getClanHall());
				winClan.setAuctionBiddedAt(0, 0);
				getClanHall().changeOwner(winner.getClanId());
				SystemMessage sm = new SystemMessage(SystemMessage.THE_CLAN_HALL_WHICH_WAS_PUT_UP_FOR_AUCTION_HAS_BEEN_AWARDED_TO_S1_CLAN).addString(winClan.getName());
				winClan.broadcastToOnlineMembers(sm);
				if(clan != null)
				{
					_log.info(this + ": end, transfer bid and deposit to owner: " + clan + " " + winner.getBid() + " + " + getDeposit());
					clan.getWarehouse().addItem("AuctionBid", 57, (long) (winner.getBid() * 0.90), winClan.getLeader().getPlayer(), clan.getLeader().getPlayer());
					clan.getWarehouse().addItem("AuctionDeposit", 57, getDeposit(), winClan.getLeader().getPlayer(), null);
					clan.broadcastToOnlineMembers(sm);
				}
			}
		}
		else
		{
			if(clan == null)
			{
				_endTask = null;
				_log.info(this + ": end, no bidders start auction again.");
				AuctionManager.getInstance().addToAuction(getClanHall());
			}
			else
			{
				_log.info(this + "end: no bidders stop auction.");
				_endTask = null;
				AuctionManager.getInstance().removeAuction(getClanHall());
				clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.THE_CLAN_HALL_WHICH_HAD_BEEN_PUT_UP_FOR_AUCTION_WAS_NOT_SOLD_AND_THEREFORE_HAS_BEEN_RELISTED));
			}
		}
	}

	@Override
	public int compareTo(Auction a)
	{
		return getClanHall().getLocation().compareTo(a.getClanHall().getLocation());
	}

	public class Bidder implements Comparable<Bidder>
	{
		private final int _clanId;
		private long _bid;
		private long _bidTime;

		public Bidder(int clanId, long bid, long bidTime)
		{
			_clanId = clanId;
			_bid = bid;
			_bidTime = bidTime;
		}

		public String getClanName()
		{
			if(_clanId > 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(_clanId);
				if(clan == null)
					return "NPC Clan";

				return clan.getName();
			}
			return "NPC Clan";
		}

		public long getBidTime()
		{
			return _bidTime;
		}

		public long getBid()
		{
			return _bid;
		}

		public int getClanId()
		{
			return _clanId;
		}

		public void updateBid(long add)
		{
			_bid += add;
			_bidTime = System.currentTimeMillis();
		}

		public void store()
		{
			Connection con = null;
			PreparedStatement statement = null;

			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("REPLACE INTO agit_auction_bid VALUES(?, ?, ?, ?)");
				statement.setInt(1, _clanHallId);
				statement.setInt(2, _clanId);
				statement.setLong(3, _bid);
				statement.setLong(4, _bidTime);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.warn(this + ": Can't store bidder: " + _clanId + " " + getClanName() + " clan hall: " + _clanHallId + " " + getClanHall().getName() + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}

		public void deleteMe()
		{
			Connection con = null;
			PreparedStatement statement = null;

			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM agit_auction_bid WHERE clan_id = ? and agit_id = ?");
				statement.setInt(1, _clanId);
				statement.setInt(2, _clanHallId);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.warn(this + ": Can't delete bidder: " + _clanId + " " + getClanName() + " clan hall: " + _clanHallId + " " + getClanHall().getName() + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}

		@Override
		public int compareTo(Bidder b)
		{
			if(b.getBid() == getBid())
			{
				if(b.getBidTime() == getBidTime())
					return 0;
				else if(b.getBidTime() < getBidTime())
					return 1;
				else
					return -1;
			}
			else if(b.getBid() > getBid())
				return 1;
			else
				return -1;
		}

		@Override
		public String toString()
		{
			return "Bidder[clan=" + ClanTable.getInstance().getClan(_clanId) + ";bid=" + _bid + ";date=" + format.format(getBidTime()) + "]";
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
		return "Auction[id=" + getClanHallId() + ";endTime=" + new Date(getEndDate().getTimeInMillis()) + "]" + getClanHall();
	}
}