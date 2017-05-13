package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.Warehouse;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2Item;

import java.util.HashMap;

/**
 * Format: cddb, b - array of (dd)
 */
public class RequestPackageSend extends L2GameClientPacket
{
	private int _objectID;
	private HashMap<Integer, Long> _items;

	private static int FREIGHT_FEE = 1000;

	@Override
	public void readImpl()
	{
		_objectID = readD();
		int itemsCount = readD();
		if(itemsCount * 12 > _buf.remaining() || itemsCount > Short.MAX_VALUE || itemsCount <= 0)
		{
			_items = null;
			return;
		}

		_items = new HashMap<>();
		for(int i = 0; i < itemsCount; i++)
		{
			int obj_id = readD(); // this is some id sent in PackageSendableList
			long itemQuantity = readQ();
			if(itemQuantity < 1)
			{
				_items = null;
				return;
			}
			_items.put(obj_id, itemQuantity);
		}
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || _items == null || AdminTemplateManager.checkBoolean("noWarehouse", player))
			return;

		if(player.getObjectId() == _objectID)
		{
			_log.warn(player + " try to send package it self cheater?!");
			return;
		}

		PcInventory inventory = player.getInventory();
		for(Integer itemObjectId : _items.keySet())
		{
			L2ItemInstance item = inventory.getItemByObjectId(itemObjectId);
			if(item == null || item.isEquipped() || item.getItem().getType2() == L2Item.TYPE2_QUEST || !item.isFreightPossible(player))
				return;

			if(_items.get(itemObjectId) <= 0)
				return;
		}

		L2NpcInstance freighter = player.getLastNpc();

		if(freighter == null || !player.isInRange(freighter, player.getInteractDistance(freighter)))
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_FAILED_AT_SENDING_THE_PACKAGE_BECAUSE_YOU_ARE_TOO_FAR_FROM_THE_WAREHOUSE));
			return;
		}

		long fee = _items.size() * (long) FREIGHT_FEE;

		if(fee > player.getAdena())
		{
			player.sendPacket(Msg.YOU_LACK_THE_FUNDS_NEEDED_TO_PAY_FOR_THIS_TRANSACTION);
			return;
		}

		L2Player destChar;
		Warehouse warehouse;
		destChar = L2Player.load(_objectID);

		if(destChar == null)
		{
			// Something went wrong!
			if(Config.DEBUG)
				_log.warn("Error retrieving a warehouse object for char " + player.getName());
			return;
		}

		destChar.restoreDisableSkills();
		warehouse = destChar.getFreight();

		// Item Max Limit Check
		if(_items.size() + warehouse.getItemsList().size() > destChar.getFreightLimit())
		{
			player.sendPacket(new SystemMessage(SystemMessage.THE_CAPACITY_OF_THE_WAREHOUSE_HAS_BEEN_EXCEEDED));
			destChar.deleteMe();
			return;
		}

		// Transfer the items from player's Inventory Instance to destChar's Freight Instance
		for(Integer itemObjectId : _items.keySet())
			inventory.transferItem("Freight", itemObjectId, _items.get(itemObjectId), warehouse, player, freighter);

		player.reduceAdena("FreightFee", fee, freighter, false);
		player.updateStats();

		// Delete destination L2Player used for freight
		destChar.deleteMe();
		player.sendPacket(new SystemMessage(SystemMessage.THE_TRANSACTION_IS_COMPLETE));
	}
}