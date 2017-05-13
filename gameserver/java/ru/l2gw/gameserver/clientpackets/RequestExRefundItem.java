package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.instances.*;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.serverpackets.ExBuyList;
import ru.l2gw.gameserver.serverpackets.ExSellRefundList;
import ru.l2gw.commons.arrays.GArray;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RequestExRefundItem extends L2GameClientPacket
{
	private int _listId;
	private int[] _items;

	/**
	 * format: d dx[d]
	 */
	@Override
	public void readImpl()
	{
		_listId = readD();
		_items = new int[readD()];
		for(int i = 0; i < _items.length; i++)
			_items[i] = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null || _listId != player.getBuyListId())
			return;

		ConcurrentLinkedQueue<L2ItemInstance> refoundList = player.getInventory().getRefundItemsList();

		if(refoundList == null || refoundList.isEmpty())
		{
			player.sendActionFailed();
			return;
		}

		L2NpcInstance npc = player.getLastNpc();

		boolean isValidMerchant = npc instanceof L2ClanBaseManagerInstance || npc instanceof L2MerchantInstance || npc instanceof L2MercManagerInstance;

		if(!player.isGM() && (npc == null || !isValidMerchant || !player.isInRange(npc, player.getInteractDistance(npc))))
		{
			player.sendActionFailed();
			return;
		}

		GArray<L2ItemInstance> toreturn = new GArray<L2ItemInstance>(_items.length);
		long price = 0, weight = 0;

		int i = 0;
		for(L2ItemInstance item : refoundList)
		{
			if(Quest.contains(_items, i))
			{
				long p = TradeController.getInstance().getBuyPrice(item.getItemId());
				price += item.getCount() * (p < 0 ? item.getReferencePrice() / 2 : p);
				weight += item.getCount() * item.getItem().getWeight();
				toreturn.add(item);
			}
			i++;
		}

		if(toreturn.isEmpty())
		{
			player.sendActionFailed();
			return;
		}

		if(player.getAdena() < price)
		{
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			player.sendActionFailed();
			return;
		}

		if(!player.getInventory().validateWeight(weight))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			player.sendActionFailed();
			return;
		}

		if(!player.getInventory().validateCapacity(toreturn))
		{
			sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			player.sendActionFailed();
			return;
		}

		if(!player.getInventory().validateWeight(weight))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			player.sendActionFailed();
			return;
		}

		player.reduceAdena("Refound", price, npc, false);

		for(L2ItemInstance itm : toreturn)
		{
			refoundList.remove(itm);
			player.getInventory().addItem("Refound", itm, player, npc);
		}

		double taxRate = 0;
		Castle castle;
		if(npc != null)
		{
			castle = npc.getCastle();
			if(castle != null)
				taxRate = castle.getTaxRate();
		}

		//player.sendPacket(new ExSellRefundList(TradeController.getInstance().getSellList(_listId), player, taxRate).done());
		player.updateStats();
		NpcTradeList list = TradeController.getInstance().getSellList(_listId);
		sendPacket(new ExBuyList(list, player, taxRate));
		sendPacket(new ExSellRefundList(player).done());
	}
}