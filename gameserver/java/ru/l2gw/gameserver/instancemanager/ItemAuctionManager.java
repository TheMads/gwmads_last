package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.entity.itemauction.AuctionItem;
import ru.l2gw.gameserver.model.entity.itemauction.ItemAuction;
import ru.l2gw.gameserver.model.entity.itemauction.ItemAuctionTemplate;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author: rage
 * @date: 28.08.2010 12:55:58
 */
public class ItemAuctionManager
{
	private static ItemAuctionManager _instance;
	private static final Log _log = LogFactory.getLog("itemauction");

	private final GArray<ItemAuctionTemplate> _templates;
	private final FastMap<Integer, ItemAuction> _auctions;

	public static ItemAuctionManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new ItemAuctionManager();
			for(ItemAuctionTemplate iat : _instance._templates)
				_instance._auctions.get(iat.getBrokerId()).startAuction();
		}
		return _instance;
	}

	private ItemAuctionManager()
	{
		_templates = new GArray<ItemAuctionTemplate>(3);
		_auctions = new FastMap<Integer, ItemAuction>().shared();
		load();
	}

	private void load()
	{
		try
		{
			_log.info(this + " loading data...");
			File file = new File(Config.ITEM_AUCTION_FILE);

			if(!file.exists())
			{
				_log.info("The " + Config.ITEM_AUCTION_FILE + " file is missing.");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				try
				{
					if("list".equalsIgnoreCase(n.getNodeName()))
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
							if("auction".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								int brokerId = Integer.parseInt(attrs.getNamedItem("brokerId").getNodeValue());
								int day = Integer.parseInt(attrs.getNamedItem("day").getNodeValue());
								int hour = Integer.parseInt(attrs.getNamedItem("hour").getNodeValue());
								int min = Integer.parseInt(attrs.getNamedItem("min") != null ? attrs.getNamedItem("min").getNodeValue() : "0");
								long time = Integer.parseInt(attrs.getNamedItem("time") != null ? attrs.getNamedItem("time").getNodeValue() : "180") * 60000L;
								ItemAuctionTemplate iat = new ItemAuctionTemplate(brokerId, day, hour, min, time);
								_templates.add(iat);
								for(Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
									if("item".equalsIgnoreCase(c.getNodeName()))
									{
										NamedNodeMap attr = c.getAttributes();
										int itemId = Integer.parseInt(attr.getNamedItem("id").getNodeValue());
										short enchant = Short.parseShort(attr.getNamedItem("enchant") != null ? attr.getNamedItem("enchant").getNodeValue() : "0");
										long startBid = Long.parseLong(attr.getNamedItem("startBid").getNodeValue());
										long count = Long.parseLong(attr.getNamedItem("count") != null ? attr.getNamedItem("count").getNodeValue() : "1");
										iat.addAuctionItem(new AuctionItem(itemId, enchant, startBid, count));
									}
							}
				}
				catch(Exception e)
				{
					_log.warn(this + " can't load auction data: " + e);
					e.printStackTrace();
				}

			_log.info(this + " loaded " + _templates.size() + " auction templates.");

			Connection con = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;

			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("SELECT * FROM `item_auction` WHERE `broker_id` = ? ORDER BY `auction_id` DESC LIMIT 1");
				for(ItemAuctionTemplate iat : _templates)
				{
					stmt.setInt(1, iat.getBrokerId());
					rs = stmt.executeQuery();
					if(rs.next())
						_auctions.put(iat.getBrokerId(), new ItemAuction(iat, rs.getInt("auction_id"), rs.getInt("item_id"), rs.getLong("current_bid"), rs.getInt("bidder_id"), rs.getLong("start_date") * 1000, rs.getLong("end_date") * 1000, rs.getInt("prev_item_id"), rs.getLong("prev_bid"), rs.getBoolean("finished")));
					else
						_auctions.put(iat.getBrokerId(), ItemAuction.createAuction(iat));
				}
			}
			catch(Exception e)
			{
				_log.warn(this + " can't restore item aution data: " + e);
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, stmt, rs);
			}

			_log.info(this + " loaded " + _auctions.size() + " auctions.");
		}
		catch(Exception e)
		{
			_log.warn(this + " Error while loading data.");
			e.printStackTrace();
		}
	}

	public void addAuction(ItemAuction ia)
	{
		_auctions.put(ia.getBrokerId(), ia);
	}

	public ItemAuction getAuctionByBrokerId(int brokerId)
	{
		return _auctions.get(brokerId);
	}
	
	public ItemAuction getAuctionById(int auctionId)
	{
		for(ItemAuction ia : _auctions.values())
			if(ia.getAuctionId() == auctionId)
				return ia;

		return null;
	}

	@Override
	public String toString()
	{
		return "ItemAuctionManager:";
	}
}
