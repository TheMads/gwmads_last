package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.Warehouse.WarehouseType;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;

public class SendWareHouseWithDrawList extends L2GameClientPacket
{
	//Format: cdb, b - array of (dd)
	private int _count;
	private long[] _items;
	private long[] counts;

	@Override
	public void readImpl()
	{
		_count = readD();
		if(_count * 12 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new long[_count * 2];
		counts = new long[_count];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 2 + 0] = readD(); //item object id
			_items[i * 2 + 1] = readQ(); //count
			if(_items[i * 2 + 0] < 1 || _items[i * 2 + 1] < 0)
			{
				_items = null;
				break;
			}
		}
	}

	@Override
	public void runImpl()
	{
		if(_items == null)
			return;

		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2NpcInstance whkeeper = player.getLastNpc();
		if(whkeeper == null || !player.isInRange(whkeeper, player.getInteractDistance(whkeeper)))
		{
			player.sendPacket(Msg.WAREHOUSE_IS_TOO_FAR);
			return;
		}
		if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
		{
			player.sendActionFailed();
			return;
		}

		if(!Config.ALT_ALLOW_OTHERS_WITHDRAW_FROM_CLAN_WAREHOUSE && player.getUsingWarehouseType() == WarehouseType.CLAN && !player.isClanLeader())
			return;

		if(player.getUsingWarehouseType() == WarehouseType.CLAN && !((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE))
			return;

		int weight = 0;
		int finalCount = player.getInventoryItemsCount();
		L2ItemInstance[] olditems = new L2ItemInstance[_count];

		for(int i = 0; i < _count; i++)
		{
			int itemObjId = (int) _items[i * 2 + 0];
			long count = _items[i * 2 + 1];
			L2ItemInstance oldinst = L2ItemInstance.restoreFromDb(itemObjId);

			if(count <= 0)
			{
				player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
				return;
			}

			if(oldinst == null)
			{
				player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.SendWareHouseWithDrawList.Changed", player));
				for(int f = 0; f < i; f++)
					L2World.removeObject(olditems[i]); // FIXME don't sure...

				return;
			}

			if(oldinst.getCount() < count)
				count = oldinst.getCount();

			counts[i] = count;
			olditems[i] = oldinst;
			weight += oldinst.getItem().getWeight() * count;
			finalCount++;

//			if(oldinst.isShadowItem())
//				oldinst.setOwner(player);

			if(oldinst.getItem().isStackable() && player.getInventory().getItemByItemId(oldinst.getItemId()) != null)
				finalCount--;
		}

		if(!player.getInventory().validateCapacity(finalCount))
		{
			for(L2ItemInstance element : olditems)
				//L2World.removeObject(items[i]);
				L2World.removeObject(element); // FIXME don't sure...
			player.sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			//            items = null;
			return;
		}

		if(!player.getInventory().validateWeight(weight))
		{
			for(L2ItemInstance element : olditems)
				L2World.removeObject(element); // FIXME don't sure...
			player.sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return;
		}

		Warehouse warehouse = null;
		if(player.getUsingWarehouseType() == WarehouseType.PRIVATE)
			warehouse = player.getWarehouse();
		else if(player.getUsingWarehouseType() == WarehouseType.CLAN)
		{
			ClanWarehousePool.getInstance().AddWork(player, olditems, counts);
			return;
		}
		else if(player.getUsingWarehouseType() == WarehouseType.FREIGHT)
			warehouse = player.getFreight();
		else
		{
			// Something went wrong!
			_log.warn("Error retrieving a warehouse object for char " + player.getName() + " - using warehouse type: " + player.getUsingWarehouseType());
			return;
		}

		for(int i = 0; i < olditems.length; i++)
			warehouse.transferItem(warehouse instanceof ClanWarehouse ? "CWhOut" : "WhOut", olditems[i].getObjectId(), counts[i], player.getInventory(), player, whkeeper);

		player.updateStats();
	}
}