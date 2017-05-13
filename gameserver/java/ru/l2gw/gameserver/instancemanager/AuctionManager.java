package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.entity.Auction;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;

public class AuctionManager
{
	protected static Log _log = LogFactory.getLog("clanhall");

	private static AuctionManager _instance;

	public static AuctionManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Initializing AuctionManager");
			_instance = new AuctionManager();
			_instance.load();
		}
		return _instance;
	}

	private static final FastMap<Integer, Auction> _auctions = new FastMap<Integer, Auction>();

	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			//mysql.set("UPDATE `agit_auction` SET `end_date` = '" + (System.currentTimeMillis() + 86400000) + "' WHERE end_date < " + System.currentTimeMillis());

			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM agit_auction ORDER BY agit_id");
			rs = statement.executeQuery();

			while(rs.next())
			{
				Auction a = new Auction(rs.getInt("agit_id"), rs.getInt("clan_id"), rs.getLong("deposit"), rs.getLong("start_bid"), rs.getLong("end_date"), rs.getString("description"));
				a.loadBidders();
				if(!a.isEndTaskStarted())
					a.endAuction();
				else
					_auctions.put(rs.getInt("agit_id"), a);
			}

			rs.close();
			statement.close();

			statement = con.prepareStatement("SELECT id FROM residence WHERE siegetype='Auction' and residenceType = 'ClanHall' AND id NOT IN (SELECT agit_id FROM agit_auction)  ORDER BY id");
			rs = statement.executeQuery();
			while(rs.next())
			{
				SiegeUnit unit = ResidenceManager.getInstance().getBuildingById(rs.getInt("id"));
				if(unit.getOwnerId() == 0)
				{
					_log.info("AuctionManager: no owner for clan hall: " + unit.getId() + " " + unit.getName() + " add to auction");
					addToAuction((ClanHall) unit);
				}
			}
			_log.info("Loaded: " + getAuctions().size() + " auction(s)");
		}
		catch(Exception e)
		{
			_log.warn("AuctionManager: can't load auctions" + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	public final Auction getAuction(int auctionId)
	{
		return _auctions.get(auctionId);
	}

	public final FastList<Auction> getAuctions()
	{
		FastList<Auction> list = new FastList<Auction>();
		if(_auctions != null)
			list.addAll(_auctions.values());
		Collections.sort(list);
		return list;
	}

	public void addToAuction(ClanHall clanHall)
	{
		Auction a = _auctions.remove(clanHall.getId());
		if(a != null)
			a.deleteMe();
		a = new Auction(clanHall.getId(), 0, 0, clanHall.getPrice(), System.currentTimeMillis() + 3 * 24 * 60 * 60000, "");
		a.store();
		_auctions.put(clanHall.getId(), a);
	}

	public void addToAuction(ClanHall clanHall, L2Clan clan, long price, int days, String desc)
	{
		Auction a = _auctions.remove(clanHall.getId());
		if(a != null)
			a.deleteMe();
		a = new Auction(clanHall.getId(), clan.getClanId(), (long) (clanHall.getPrice() * 0.50), price, System.currentTimeMillis() + days * 24 * 60 * 60000, desc);
		a.store();
		_auctions.put(clanHall.getId(), a);
	}

	public void removeAuction(ClanHall clanHall)
	{
		_log.info("AuctionManager: remove from auction: " + clanHall);
		Auction a = _auctions.remove(clanHall.getId());
		if(a != null)
			a.deleteMe();
	}
}