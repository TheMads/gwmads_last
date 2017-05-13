package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.CastleManorManager;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.l2gw.gameserver.model.L2Manor;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2ManorManagerInstance;
import ru.l2gw.gameserver.serverpackets.StatusUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

public class RequestProcureCrop extends L2GameClientPacket
{
	// format: cddb
	private int _listId;
	private int _count;
	private long[] _items;
	private GArray<CropProcure> _procureList = new GArray<CropProcure>();

	@Override
	protected void readImpl()
	{
		_listId = readD();
		_count = readD();
		if(_count * 16 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_count = 0;
			return;
		}
		_items = new long[_count * 2];
		for(int i = 0; i < _count; i++)
		{
			long servise = readD();
			_items[i * 2 + 0] = readD();
			long cnt = readQ();
			if(cnt < 1)
			{
				_count = 0;
				_items = null;
				return;
			}
			_items[i * 2 + 1] = cnt;
		}
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		// Alt game - Karma punishment
		if(!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0)
			return;

		L2Object target = player.getTarget();

		if(_count < 1 || _items == null)
		{
			player.sendActionFailed();
			return;
		}

		long subTotal = 0;
		int tax = 0;

		// Check for buylist validity and calculates summary values
		int slots = 0;
		int weight = 0;
		L2ManorManagerInstance manor = target != null && target instanceof L2ManorManagerInstance ? (L2ManorManagerInstance) target : null;

		for(int i = 0; i < _count; i++)
		{
			int itemId = (int) _items[i * 2 + 0];
			long count = _items[i * 2 + 1];
			if(count < 0 || count > Integer.MAX_VALUE)
			{
				sendPacket(Msg.INCORRECT_ITEM_COUNT);
				return;
			}

			L2Item template = ItemTable.getInstance().getTemplate(L2Manor.getInstance().getRewardItem(itemId, manor.getCastle().getCrop(itemId, CastleManorManager.PERIOD_CURRENT).getReward()));
			weight += count * template.getWeight();

			if(!template.isStackable())
				slots += count;
			else if(player.getInventory().getItemByItemId(itemId) == null)
				slots++;
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
		_procureList = manor.getCastle().getCropProcure(CastleManorManager.PERIOD_CURRENT);

		for(int i = 0; i < _count; i++)
		{
			int itemId = (int) _items[i * 2 + 0];
			long count = _items[i * 2 + 1];
			if(count < 0)
				count = 0;

			int rewradItemId = L2Manor.getInstance().getRewardItem(itemId, manor.getCastle().getCrop(itemId, CastleManorManager.PERIOD_CURRENT).getReward());

			long rewradItemCount = L2Manor.getInstance().getRewardAmountPerCrop(manor.getCastle().getId(), itemId, manor.getCastle().getCropRewardType(itemId));

			rewradItemCount = count * rewradItemCount;

			// Add item to Inventory and adjust update packet
			player.destroyItemByItemId("Manor", itemId, count, player.getLastNpc(), true);
			L2ItemInstance item = player.getInventory().addItem("Manor", rewradItemId, rewradItemCount, player, player.getLastNpc());

			if(item == null)
				continue;

			// Send Char Buy Messages
			SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1_S);
			sm.addItemName(rewradItemId);
			sm.addNumber(rewradItemCount);
			player.sendPacket(sm);
			sm = null;

			//manor.getCastle().setCropAmount(itemId, manor.getCastle().getCrop(itemId, CastleManorManager.PERIOD_CURRENT).getAmount() - count);
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}
