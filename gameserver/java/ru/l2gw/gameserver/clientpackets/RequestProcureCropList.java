package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.CastleManorManager;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Manor;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2ManorManagerInstance;
import ru.l2gw.gameserver.serverpackets.StatusUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

/**
 * Format: (ch) d [dddd]
 * d: size
 * [
 * d  obj id
 * d  item id
 * d  manor id
 * d  count
 * ]
 *
 */
public class RequestProcureCropList extends L2GameClientPacket
{
	private int _size;
	private long[] _items; // count*4

	@Override
	protected void readImpl()
	{
		_size = readD();
		if(_size * 20 > _buf.remaining() || _size > Short.MAX_VALUE || _size <= 0)
		{
			_size = 0;
			return;
		}
		_items = new long[_size * 4];
		for(int i = 0; i < _size; i++)
		{
			_items[i * 4 + 0] = readD();
			_items[i * 4 + 1] = readD();
			_items[i * 4 + 2] = readD();
			_items[i * 4 + 3] = readQ();
			if(_items[i * 4 + 0] < 1 || _items[i * 4 + 1] < 1 || _items[i * 4 + 2] < 1 || _items[i * 4 + 3] < 1)
			{
				_size = 0;
				_items = null;
				return;
			}
		}
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(_size < 1)
		{
			player.sendActionFailed();
			return;
		}

		L2Object target = player.getTarget();

		if(!(target instanceof L2ManorManagerInstance))
			target = player.getLastNpc();

		if(!player.isGM() && (target == null || !(target instanceof L2ManorManagerInstance) || !player.isInRange(target, player.getInteractDistance(target))))
			return;

		L2ManorManagerInstance manorManager = (L2ManorManagerInstance) target;

		int currentManorId = manorManager.getCastle().getId();

		// Calculate summary values
		int slots = 0;
		int weight = 0;

		for(int i = 0; i < _size; i++)
		{
			int itemId = (int) _items[i * 4 + 1];
			int manorId = (int)_items[i * 4 + 2];
			long count = _items[i * 4 + 3];

			if(itemId == 0 || manorId == 0 || count == 0)
				continue;
			if(count < 1)
				continue;
			Castle castle = ResidenceManager.getInstance().getCastleById(manorId);
			if(!castle.isCastle)
				return;
			if(count > Integer.MAX_VALUE)
			{
				sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED));
				return;
			}

			try
			{
				CropProcure crop = castle.getCrop(itemId, CastleManorManager.PERIOD_CURRENT);
				int rewardItemId = L2Manor.getInstance().getRewardItem(itemId, crop.getReward());
				L2Item template = ItemTable.getInstance().getTemplate(rewardItemId);
				weight += count * template.getWeight();

				if(!template.isStackable())
					slots += count;
				else if(player.getInventory().getItemByItemId(itemId) == null)
					slots++;
			}
			catch(NullPointerException e)
			{
				continue;
			}
		}

		if(!player.getInventory().validateWeight(weight))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return;
		}

		if(!player.getInventory().validateCapacity(slots))
		{
			sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			return;
		}

		// Proceed the purchase
		for(int i = 0; i < _size; i++)
		{
			int objId = (int) _items[i * 4 + 0];
			int cropId = (int) _items[i * 4 + 1];
			int manorId = (int) _items[i * 4 + 2];
			long count = _items[i * 4 + 3];

			if(objId == 0 || cropId == 0 || manorId == 0 || count == 0)
				continue;

			if(count < 1)
				continue;

			CropProcure crop = null;
			Castle castle = ResidenceManager.getInstance().getCastleById(manorId);
			if(!castle.isCastle)
				return;
			try
			{
				crop = castle.getCrop(cropId, CastleManorManager.PERIOD_CURRENT);
			}
			catch(NullPointerException e)
			{
				continue;
			}
			if(crop == null || crop.getId() == 0 || crop.getPrice() == 0)
				continue;

			long fee = 0; // fee for selling to other manors

			int rewardItem = L2Manor.getInstance().getRewardItem(cropId, crop.getReward());

			if(count > crop.getAmount())
				continue;

			long sellPrice = count * crop.getPrice();
			int rewardPrice = ItemTable.getInstance().getTemplate(rewardItem).getReferencePrice();

			if(rewardPrice == 0)
				continue;

			long rewardItemCount = sellPrice / rewardPrice;
			if(rewardItemCount < 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessage.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(cropId);
				sm.addNumber(count);
				player.sendPacket(sm);
				continue;
			}

			if(manorId != currentManorId)
				fee = sellPrice * 5 / 100; // 5% fee for selling to other manor

			if(player.getInventory().getAdena() < fee)
			{
				SystemMessage sm = new SystemMessage(SystemMessage.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(cropId);
				sm.addNumber(count);
				player.sendPacket(sm);
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				continue;
			}

			// Add item to Inventory and adjust update packet
			L2ItemInstance itemAdd;
			if(player.getInventory().getItemByObjectId(objId) == null)
				continue;

			// check if player have correct items count
			if(!player.destroyItem("Manor", objId, count, null, false))
				continue;

			if(fee > 0)
				player.reduceAdena("Manor", fee, null, false);

			crop.setAmount(crop.getAmount() - count);
			// TODO update SQL for long
			if(Config.MANOR_SAVE_ALL_ACTIONS)
				castle.updateCrop(crop.getId(), crop.getAmount(), CastleManorManager.PERIOD_CURRENT);

			itemAdd = player.getInventory().addItem("Manor", rewardItem, rewardItemCount, player, null);
			if(itemAdd == null)
				continue;

			// Send System Messages
			SystemMessage sm = new SystemMessage(SystemMessage.TRADED_S2_OF_CROP_S1);
			sm.addItemName(cropId);
			sm.addNumber(count);
			player.sendPacket(sm);

			if(fee > 0)
			{
				sm = new SystemMessage(SystemMessage.S1_ADENA_HAS_BEEN_PAID_FOR_PURCHASING_FEES);
				sm.addNumber(fee);
				player.sendPacket(sm);
			}

			sm = new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED);
			sm.addItemName(cropId);
			sm.addNumber(count);
			player.sendPacket(sm);

			if(fee > 0)
			{
				sm = new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED);
				sm.addNumber(fee);
				player.sendPacket(sm);
			}

			sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1_S);
			sm.addItemName(rewardItem);
			sm.addNumber(rewardItemCount);
			player.sendPacket(sm);
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}
