package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance.ItemLocation;
import ru.l2gw.gameserver.tables.ItemTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Warehouse extends Inventory
{
	public static enum WarehouseType
	{
		PRIVATE(1),
		CLAN(2),
		CASTLE(3),
		FREIGHT(4);

		private final int _type;

		private WarehouseType(final int type)
		{
			_type = type;
		}

		public int getPacketValue()
		{
			return _type;
		}
	}

	private boolean restored = false;

	private static final Log _log = LogFactory.getLog(Warehouse.class.getName());

	public abstract int getOwnerId();

	public abstract ItemLocation getLocationType();

	public abstract WarehouseType getWarehouseType();

	public abstract String getLocationId();

	public abstract byte getLocationId(boolean addItem);

	public abstract void setLocationId(L2Player player);

	public L2ItemInstance[] getItems()
	{
		if(!restored)
			restoreWh();

		return _items.toArray(new L2ItemInstance[_items.size()]);
	}

	@Override
	public ConcurrentLinkedQueue<L2ItemInstance> getItemsList()
	{
		if(!restored)
			restoreWh();

		return _items;
	}

	/**
	 * Adds item to inventory
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param item	  : L2ItemInstance to be added
	 * @param actor	 : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public synchronized L2ItemInstance addItem(String process, L2ItemInstance item, L2Player actor, L2Object reference)
	{
		L2ItemInstance olditem = getItemByItemId(item.getItemId());

		if(olditem != null && olditem.isStackable())
		{
			long count = item.getCount();
			olditem.changeCount(process, count, actor, reference);
			olditem.setLastChange(L2ItemInstance.MODIFIED);

			// And destroys the item
			ItemTable.getInstance().destroyItem(process, item, actor, reference);
			item.updateDatabase(true);
			item = olditem;
			item.updateDatabase(true);
		}
		else
		{
			item.setOwnerId(getOwnerId());
			item.changeLocation(process, getLocationType(), actor, reference);
			item.setLastChange((L2ItemInstance.ADDED));
			item.updateDatabase(true);
			addItem(item);
			item.prepareRemove();
		}

		return item;
	}

	/**
	 * Destroy item from inventory and updates database
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param item	  : L2ItemInstance to be destroyed
	 * @param actor	 : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, L2Player actor, L2Object reference)
	{
		if(!restored)
			restoreWh();
		synchronized(item)
		{
			ItemTable.getInstance().destroyItem(process, item, actor, reference);
			item.updateDatabase(true);
			_items.remove(item);
		}
		return item;
	}

	/**
	 * Returns the item from inventory by using its <B>itemId</B>
	 *
	 * @param itemId : int designating the ID of the item
	 * @return L2ItemInstance designating the item or null if not found in
	 *         inventory
	 */
	@Override
	public L2ItemInstance getItemByItemId(int itemId)
	{
		if(!restored)
			restoreWh();

		return super.getItemByItemId(itemId);
	}

	/**
	 * Returns the item from inventory by using its <B>itemId</B>
	 *
	 * @param object : int designating the ID of the item
	 * @return L2ItemInstance designating the item or null if not found in
	 *         inventory
	 */
	@Override
	public L2ItemInstance getItemByObjectId(Integer objectId)
	{
		if(!restored)
			restoreWh();

		return super.getItemByObjectId(objectId);
	}

	@Override
	protected void sendModifyItem(L2ItemInstance item)
	{
	}

	@Override
	protected void sendRemoveItem(L2ItemInstance item)
	{
	}

	@Override
	protected void sendNewItem(L2ItemInstance item)
	{
	}

	private synchronized void restoreWh()
	{
		if(!restored)
		{
			restored = true;
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;

			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT * FROM items WHERE owner_id=? AND loc=? LIMIT 200");
				statement.setInt(1, getOwnerId());
				statement.setString(2, getLocationType().name());
				rset = statement.executeQuery();
				L2Player owner = L2ObjectsStorage.getPlayer(getOwnerId());

				L2ItemInstance item;
				while(rset.next())
				{
					item = L2ItemInstance.restoreFromDb(rset);
					if(item == null)
						continue;

					if(item.isHeroItem() && owner != null && !owner.isHero())
					{
						ItemTable.getInstance().destroyItem("HeroItem", item, null, null);
						item.updateDatabase();
						continue;
					}
					if(!_items.contains(item))
						_items.add(item);
				}
			}
			catch(final Exception e)
			{
				_log.error("could not restore warehouse:", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
	}
}