package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.ClanWarehouse;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.Warehouse;
import ru.l2gw.gameserver.model.Warehouse.WarehouseType;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SendWareHouseDepositList extends L2GameClientPacket
{
	//Format: cdb, b - array of (dd)
	private static int _WAREHOUSE_FEE = 30;
	private HashMap<Integer, Long> _items;

	@Override
	public void readImpl()
	{
		int itemsCount = readD();
		if(itemsCount * 12 > _buf.remaining() || itemsCount > Short.MAX_VALUE || itemsCount < 0)
		{
			_items = null;
			return;
		}
		_items = new HashMap<Integer, Long>(itemsCount + 1, 0.999f);
		for(int i = 0; i < itemsCount; i++)
		{
			int obj_id = readD();
			long itemQuantity = readQ();
			if(obj_id < 1 || itemQuantity < 0)
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
		if(player == null || _items == null)
			return;
			
		if (player.isWhDisabled())
			return;
		

		// Проверяем наличие npc и расстояние до него
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

		Warehouse warehouse;
		PcInventory inventory = player.getInventory();
		boolean privatewh = player.getUsingWarehouseType() != WarehouseType.CLAN;
		int slotsleft = 0;
		long adenaDeposit = 0;

		// Список предметов, уже находящихся на складе
		ConcurrentLinkedQueue<L2ItemInstance> itemsOnWarehouse;
		if(privatewh)
		{
			warehouse = player.getWarehouse();
			itemsOnWarehouse = warehouse.getItemsList();
			slotsleft = player.getWarehouseLimit() - itemsOnWarehouse.size();
		}
		else
		{
			warehouse = player.getClan().getWarehouse();
			itemsOnWarehouse = warehouse.getItemsList();
			slotsleft = Config.WAREHOUSE_SLOTS_CLAN - itemsOnWarehouse.size();
		}

		// Список стекуемых предметов, уже находящихся на складе
		ArrayList<Integer> stackableList = new ArrayList<Integer>();
		for(L2ItemInstance i : itemsOnWarehouse)
			if(i.isStackable())
				stackableList.add(i.getItemId());

		// Создаем новый список передаваемых предметов, на основе полученных данных
		ArrayList<L2ItemInstance> itemsToStoreList = new ArrayList<L2ItemInstance>(_items.size() + 1);
		for(Integer itemObjectId : _items.keySet())
		{
			L2ItemInstance item = inventory.getItemByObjectId(itemObjectId);
			if(item == null || item.isEquipped() || item.getCount() <= 0)
				continue;
			if(!privatewh && !item.canBeStored(player, privatewh)) // а его вообще положить можно?
				continue;
			if(!item.isStackable() || !stackableList.contains(item.getItemId())) // вещь требует слота
			{
				if(slotsleft <= 0) // если слоты кончились нестекуемые вещи и отсутствующие стекуемые пропускаем
					continue;
				slotsleft--; // если слот есть то его уже нет
			}
			if(item.getItemId() == 57)
				adenaDeposit = _items.get(itemObjectId);
			itemsToStoreList.add(item);
		}

		// Проверяем, хватит ли у нас денег на уплату налога
		int fee = itemsToStoreList.size() * _WAREHOUSE_FEE;
		if(fee + adenaDeposit > player.getAdena())
		{
			player.sendPacket(Msg.YOU_LACK_THE_FUNDS_NEEDED_TO_PAY_FOR_THIS_TRANSACTION);
			return;
		}

		// Сообщаем о том, что слоты кончились
		if(slotsleft <= 0)
			player.sendPacket(Msg.YOUR_WAREHOUSE_IS_FULL);

		// Перекидываем
		for(L2ItemInstance itemToStore : itemsToStoreList)
			inventory.transferItem(warehouse instanceof ClanWarehouse ? "CWhIn" : "WhIn", itemToStore.getObjectId(), _items.get(itemToStore.getObjectId()), warehouse, player, whkeeper);

		// Платим налог
		player.reduceAdena("WHFee", fee, whkeeper, false);

		// Обновляем параметры персонажа
		player.updateStats();
	}
}