package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TransferManager
{
	private boolean _running = false;
	private static final String CHECK_ITEMS = "SELECT t. * , c.obj_id FROM `transfer` t INNER JOIN characters c ON ( t.char_name = c.char_name ) WHERE `status`=0";
	private static final String GET_ITEM = "SELECT * FROM items WHERE owner_id=? and item_id=? and loc='INVENTORY'";
	private static final String UPDATE_ITEM_COUNT = "UPDATE items SET count = count + ? WHERE owner_id = ? and item_id = ? and loc='INVENTORY'";
	private static final String UPDATE_TRANSFER = "UPDATE transfer SET `status` = 1, utdt = now() WHERE `id` = ?";
	private static final Log _log = LogFactory.getLog("transferManager");

	private static TransferManager _instance;

	public static TransferManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Initializing TransferManager");
			_instance = new TransferManager();
		}
		return _instance;
	}

	public TransferManager()
	{
		if(Config.DEBUG)
			_log.info("TransferManager: set to " + Config.TM_PERIOD / 1000 + " sec");
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TransferTask(), Config.TM_PERIOD, Config.TM_PERIOD);
	}

	protected class TransferTask extends Thread
	{
		public void run()
		{
			if(!_running) TransferItems();
		}
	}

	public void TransferItems()
	{

		_running = true;

		Connection con = null;
		int id = 0;
		int itemId = 0;
		long amount = 0;
		int obj_id = 0;
		String char_name = "";
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(CHECK_ITEMS);
			ResultSet rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while(rset.next())
			{
				id = rset.getInt("id");
				itemId = rset.getInt("itemid");
				amount = rset.getInt("amount");
				char_name = rset.getString("char_name");
				obj_id = rset.getInt("obj_id");

				_log.warn("transfer[" + id + "]: itemId: " + itemId + " amount: " + amount + " to: " + char_name + "[" + obj_id + "]");

				L2Player player = L2ObjectsStorage.getPlayer(char_name);

				if(player != null)
				{
					_log.warn("transfer[" + id + "]: " + char_name + " online");

					player.addItem("WebTransfer", itemId, amount, player, true);

					player.getInventory().sendItemList(true);

					PreparedStatement stmt = con.prepareStatement(UPDATE_TRANSFER);
					stmt.setInt(1, id);
					stmt.execute();
					stmt.close();
				}
				else
				{
					_log.warn("transfer[" + id + "]: " + char_name + " offline");

					PreparedStatement stmt = con.prepareStatement(GET_ITEM);
					stmt.setInt(1, obj_id);
					stmt.setInt(2, itemId);
					ResultSet rset1 = stmt.executeQuery();

					long count = 0;
					if(rset1.next())
						count = rset1.getLong("count");

					rset1.last();

					if(rset1.getRow() > 0)
					{
						_log.warn("transfer[" + id + "]: item exists, update count");

						stmt.close();

						stmt = con.prepareStatement(UPDATE_ITEM_COUNT);
						if((count + amount) > L2Item.MAX_COUNT)
						{
							_log.warn("transfer[" + id + "]: total amount of items more then MAX_INT " + char_name + " itemId: " + itemId + " amount: " + amount + " count: " + count + " objId: " + obj_id);
							amount = L2Item.MAX_COUNT - count;
							_log.warn("transfer[" + id + "]: new amount " + amount);
						}
						stmt.setLong(1, amount);
						stmt.setInt(2, obj_id);
						stmt.setInt(3, itemId);
						stmt.execute();
						stmt.close();
					}
					else
					{
						stmt.close();
						if(Config.DEBUG)
							_log.warn("transfer[" + id + "]: item dont exists, create itemId "+itemId+" amount: "+amount);

						L2Item template = ItemTable.getInstance().getTemplate(itemId);

						if(template == null)
						{
							_log.warn("transfer[" + id + "]: invalid itemId " + itemId);
							continue;
						}

						L2ItemInstance item = ItemTable.getInstance().createItem("WebTransfer", itemId, amount, null, null);
						item.setOwnerId(obj_id);
						item.setLocation(L2ItemInstance.ItemLocation.INVENTORY);
						item.setLastChange(L2ItemInstance.ADDED);
						item.updateDatabase(true);
					}
					rset1.close();

					stmt = con.prepareStatement(UPDATE_TRANSFER);
					stmt.setInt(1, id);
					stmt.execute();
					stmt.close();
				}
			}

			rset.close();
			statement.close();
			_running = false;
		}
		catch(Exception e)
		{
			_log.warn("TransferManager: transfer error to " + char_name + " itemId: " + itemId + " amount: " + amount + " objId: " + obj_id + " id " + id);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
				_running = false;
			}
			catch(Exception e)
			{
			}
		}
	}
}