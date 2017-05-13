package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PremiumItem;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author: rage
 * @date: 21.01.13 18:26
 */
public class PremiumItemManager
{
	private static final Log log = LogFactory.getLog("premium_manager");
	private static final Map<Integer, List<PremiumItem>> premiumItems = new HashMap<>();
	private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private static final Lock readLock = readWriteLock.readLock();
	private static final Lock writeLock = readWriteLock.writeLock();

	public static void startLoadTask()
	{
		log.info("PremiumItemManager: started.");
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				loadNewItems();
			}
		}, Config.PREMIUM_MANAGER_ITEMS_LOAD_DELAY * 1000, Config.PREMIUM_MANAGER_ITEMS_LOAD_DELAY * 1000);
	}

	public static void onPlayerEnter(L2Player player)
	{
		loadItemsByObjectId(player.getObjectId());
		readLock.lock();
		try
		{
			if(premiumItems.get(player.getObjectId()).size() > 0)
				player.sendPacket(Msg.ExNotifyPremiumItem);
		}
		finally
		{
			readLock.unlock();
		}
	}

	private static void loadItemsByObjectId(int objectId)
	{
		readLock.lock();
		try
		{
			if(premiumItems.containsKey(objectId))
				return;
		}
		finally
		{
			readLock.unlock();
		}

		writeLock.lock();
		try
		{
			List<PremiumItem> items = new ArrayList<>();
			premiumItems.put(objectId, items);

			Connection con = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;

			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("SELECT * FROM premium_items WHERE owner_id = ?");
				stmt.setInt(1, objectId);
				rs = stmt.executeQuery();

				while(rs.next())
					items.add(new PremiumItem(rs.getInt("id"), rs.getInt("item_id"), rs.getInt("owner_id"), rs.getLong("amount"), rs.getString("sender")));

				DbUtils.closeQuietly(stmt);
				stmt = con.prepareStatement("UPDATE premium_items SET status = 1 WHERE owner_id = ?");
				stmt.setInt(1, objectId);
				stmt.execute();
			}
			catch(Exception e)
			{
				log.error("PremiumItemManager: could not restore items for player: " + objectId + ":" + e, e);
			}
			finally
			{
				DbUtils.closeQuietly(con, stmt, rs);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	private static void loadNewItems()
	{
		writeLock.lock();
		try
		{
			Connection con = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;

			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("SELECT * FROM premium_items WHERE status = 0");
				rs = stmt.executeQuery();
				List<PremiumItem> items = new ArrayList<>();

				while(rs.next())
				{
					items.add(new PremiumItem(rs.getInt("id"), rs.getInt("item_id"), rs.getInt("owner_id"), rs.getLong("amount"), rs.getString("sender")));
				}

				if(items.size() > 0)
				{
					DbUtils.closeQuietly(stmt);
					stmt = con.prepareStatement("UPDATE premium_items SET status = 1 WHERE id = ?");
					Set<Integer> players = new HashSet<>();

					for(PremiumItem item : items)
					{
						stmt.setInt(1, item.getId());
						stmt.addBatch();

						List<PremiumItem> userItems = premiumItems.get(item.getOwnerId());
						if(userItems == null)
						{
							userItems = new ArrayList<>();
							premiumItems.put(item.getOwnerId(), userItems);
						}

						userItems.add(item);
						players.add(item.getOwnerId());
					}
					stmt.executeBatch();

					for(Integer objectId : players)
					{
						L2Player player = L2ObjectsStorage.getPlayer(objectId);
						if(player != null && !player.isInOfflineMode())
							player.sendPacket(Msg.ExNotifyPremiumItem);
					}
				}
			}
			catch(Exception e)
			{
				log.error("PremiumItemManager: could not load items: " + e, e);
			}
			finally
			{
				DbUtils.closeQuietly(con, stmt, rs);
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public static List<PremiumItem> getItemsByObjectId(int objectId)
	{
		readLock.lock();
		try
		{
			if(premiumItems.containsKey(objectId))
			{
				List<PremiumItem> items = premiumItems.get(objectId);
				List<PremiumItem> list = new ArrayList<>(items.size());
				list.addAll(items);
				return list;
			}
		}
		finally
		{
			readLock.unlock();
		}

		return Collections.emptyList();
	}

	public static PremiumItem getPremiumItem(int ownerId, int id)
	{
		readLock.lock();
		try
		{
			List<PremiumItem> items = premiumItems.get(ownerId);
			if(items == null)
				return null;

			for(PremiumItem item : items)
				if(item.getId() == id)
					return item;
		}
		finally
		{
			readLock.unlock();
		}

		return null;
	}

	public static boolean withdrawItem(int ownerId, int id, long count)
	{
		if(count <= 0)
			return false;

		writeLock.lock();
		try
		{
			if(!premiumItems.containsKey(ownerId))
				return false;

			List<PremiumItem> items = premiumItems.get(ownerId);
			PremiumItem item = null;
			for(PremiumItem premiumItem : items)
			{
				if(premiumItem.getId() == id)
				{
					item = premiumItem;
					break;
				}
			}

			if(item == null)
				return false;

			if(item.getCount() < count)
				return false;

			item.setCount(item.getCount() - count);

			if(item.getCount() == 0)
				items.remove(item);

			updateItem(item);
		}
		catch(Exception e)
		{
			log.error("PremiumManager: can't update premium item: " + id + " owner_id: " + ownerId + " count: " + count + " " + e, e);
			return false;
		}
		finally
		{
			writeLock.unlock();
		}

		return true;
	}

	public static void sendItemToPlayer(int ownerId, int itemId, long count, String sender)
	{
		writeLock.lock();
		try
		{
			List<PremiumItem> items = premiumItems.get(ownerId);
			if(items == null)
			{
				items = new ArrayList<>();
				premiumItems.put(ownerId, items);
			}
			L2Item itemTemplate = ItemTable.getInstance().getTemplate(itemId);
			if(itemTemplate == null)
				return;

			if(itemTemplate.isStackable())
			{
				PremiumItem item = null;
				for(PremiumItem premiumItem : items)
					if(premiumItem.getItemId() == itemId)
					{
						item = premiumItem;
						break;
					}

				if(item != null)
				{
					item.setCount(item.getCount() + count);
					updateItem(item);
				}
				else
				{
					item = new PremiumItem(0, itemId, ownerId, count, sender);
					insertItem(item);
					items.add(item);
				}
			}
			else
			{
				PremiumItem item = new PremiumItem(0, itemId, ownerId, count, sender);
				insertItem(item);
				items.add(item);
			}
		}
		catch(Exception e)
		{
			log.error("PremiumItemManager: can't send item to ownerId: " + ownerId + " itemId: " + itemId + " count: " + count + " sender: " + sender + " " + e, e);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	private static void updateItem(PremiumItem item) throws Exception
	{
		Connection con = null;
		PreparedStatement stmt = null;

		con = DatabaseFactory.getInstance().getConnection();
		if(item.getCount() <= 0)
		{
			stmt = con.prepareStatement("DELETE FROM premium_items WHERE id = ?");
			stmt.setInt(1, item.getId());
		}
		else
		{
			stmt = con.prepareStatement("UPDATE premium_items SET amount = ? WHERE id = ?");
			stmt.setLong(1, item.getCount());
			stmt.setInt(2, item.getId());
		}

		stmt.execute();
		DbUtils.closeQuietly(con, stmt);
	}

	private static void insertItem(PremiumItem item) throws Exception
	{
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		con = DatabaseFactory.getInstance().getConnection();
		stmt = con.prepareStatement("INSERT INTO premium_items(item_id, owner_id, amount, sender, status) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
		stmt.setInt(1, item.getItemId());
		stmt.setInt(2, item.getOwnerId());
		stmt.setLong(3, item.getCount());
		stmt.setString(4, item.getSender());
		stmt.setInt(5, 1);
		stmt.executeUpdate();
		rs = stmt.getGeneratedKeys();
		if(rs.next())
			item.setId(rs.getInt(1));

		DbUtils.closeQuietly(con, stmt, rs);
	}
}
