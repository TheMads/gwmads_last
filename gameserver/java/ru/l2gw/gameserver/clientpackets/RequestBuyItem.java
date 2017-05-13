package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.SafeMath;
import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.*;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.serverpackets.ExBuyList;
import ru.l2gw.gameserver.serverpackets.ExSellRefundList;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.util.Util;

public class RequestBuyItem extends L2GameClientPacket
{
	private static org.apache.commons.logging.Log _log = LogFactory.getLog(RequestBuyItem.class);
	private int _listId;
	private int _count;
	private long[] _items; // count*2

	/**
	 * packet type id 0x40
	 *
	 * sample
	 *
	 * 1f
	 * 44 22 02 01		// list id
	 * 02 00 00 00		// items to buy
	 *
	 * 27 07 00 00		// item id
	 * 06 00 00 00		// count
	 *
	 * 83 06 00 00
	 * 01 00 00 00
	 *
	 * format:		cddb, b - array of (dd)
	 */
	@Override
	public void readImpl()
	{
		_listId = readD();
		_count = readD();
		if(_count * 12 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new long[_count * 2];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 2 + 0] = readD();
			_items[i * 2 + 1] = readQ();
			if(_items[i * 2 + 0] < 0 || _items[i * 2 + 1] < 1)
			{
				_items = null;
				break;
			}
		}
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		if(_items == null || _count == 0)
			return;

		if(player.isOutOfControl())
		{
			player.sendActionFailed();
			return;
		}

		if(!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0 && !player.isGM())
		{
			player.sendActionFailed();
			return;
		}
		if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
		{
			player.sendActionFailed();
			return;
		}

		if(player.getBuyListId() != _listId)
		{
			_log.warn(player + " try to buy from wong sell list: " + _listId + " last list id: " + player.getBuyListId());
			player.sendActionFailed();
			return;
		}

		L2NpcInstance npc = player.getLastNpc();

		boolean isValidMerchant = npc instanceof L2ClanBaseManagerInstance || npc instanceof L2MerchantInstance || npc instanceof L2MercManagerInstance || npc instanceof L2CastleChamberlainInstance;

		if(!player.isGM() && (npc == null || !isValidMerchant || !player.isInRange(npc, player.getInteractDistance(npc))))
		{
			player.sendActionFailed();
			return;
		}

		L2NpcInstance merchant = null;
		if(npc != null && (npc instanceof L2MerchantInstance || npc instanceof L2ClanBaseManagerInstance)) //TODO расширить список?
			merchant = npc;

		NpcTradeList sellList = TradeController.getInstance().getSellList(_listId);

		if(sellList == null)
		{
			_log.warn(player + " try to buy from not existing sell list: " + _listId);
			player.sendActionFailed();
			return;
		}

		if(merchant != null && sellList.getNpcId() != merchant.getNpcId())
		{
			_log.warn(player + " try to buy from sell list: " + _listId + " with wrong npc id: " + merchant.getNpcId() + " sell list npc id: " + sellList.getNpcId());
			player.sendActionFailed();
			return;
		}

		long finalLoad = 0;
		int finalCount = player.getInventoryItemsCount();
		int weight;

		int itemId;
		long cnt, price, tax = 0, subTotal = 0, totalCost = 0;
		double taxRate = 0;

		if(merchant != null && (!Config.SERVICES_OFFSHORE_NO_CASTLE_TAX && !merchant.isInZone(ZoneType.offshore)))
			taxRate = merchant.getCastle().getTaxRate();

		for(int i = 0; i < _count; i++)
		{
			itemId = (int) _items[i * 2];
			cnt = _items[i * 2 + 1];

			if(cnt <= 0)
			{
				player.sendActionFailed();
				return;
			}

			TradeItem ti = sellList.getTradeItem(itemId);
			if(ti == null)
			{
				Util.handleIllegalPlayerAction(player, "RequestBuyItem[151]", "tried to purchase item: " + itemId + " that is not in sell list: " + sellList.getListId() + " npc: " + merchant, 2);
				player.sendActionFailed();
				return;
			}

			L2Item item = ItemTable.getInstance().getTemplate(itemId);

			if(!item.isStackable() && cnt != 1)
			{
				Util.handleIllegalPlayerAction(player, "RequestBuyItem[160]", "tried to purchase " + cnt + " non-stackable items: " + itemId + " to 1 slot", 2);
				player.sendActionFailed();
				return;
			}

			if(ti.getLimitCount() > 0 && ti.getCount() < cnt)
			{
				player.sendActionFailed();
				return;
			}

			price = ti.getOwnersPrice();

			if(price == 0 && !AdminTemplateManager.checkBoolean("zeroBuy", player))
			{
				Util.handleIllegalPlayerAction(player, "RequestBuyItem[175]", "Tried to use GMShop: " + _listId + " item " + itemId, 2);
				player.sendActionFailed();
				return;
			}
			else if(price == 0 && Config.DISABLE_CREATION_ID_LIST.contains(itemId))
				return;

			if(itemId >= 3960 && itemId <= 4921)
				price *= Config.RATE_SIEGE_GUARDS_PRICE;

			weight = item.getWeight();

			if(price < 0)
			{
				_log.warn(player + " try to buy item: " + itemId + " from npc: " + merchant + " list id: " + _listId + " price: " + price);
				player.sendActionFailed();
				return;
			}

			try
			{
				subTotal = SafeMath.safeAddLong(subTotal, SafeMath.safeMulLong(cnt, price)); // Before tax
				tax = SafeMath.safeMulLong(subTotal, taxRate);
				totalCost = SafeMath.safeAddLong(subTotal, tax);

				if(totalCost < 0)
					throw new ArithmeticException("201: Tried to purchase negative " + totalCost + " adena worth of goods.");

				finalLoad = SafeMath.safeAddLong(finalLoad, SafeMath.safeMulLong(cnt, weight));
				if(finalLoad < 0)
					throw new ArithmeticException("205: Tried to purchase negative " + finalLoad + " adena worth of goods.");
			}
			catch(ArithmeticException e)
			{
				Util.handleIllegalPlayerAction(player, "RequestBuyItem[209]", "merchant: " + merchant + ": " + e.getMessage(), 1);
				sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
				sendPacket(Msg.ActionFail);
				return;
			}

			if(!item.isStackable() || player.getInventory().getItemByItemId(itemId) == null)
				finalCount++;
		}

		if(totalCost > player.getAdena() || subTotal < 0)
		{
			sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			player.sendActionFailed();
			return;
		}

		if(!player.getInventory().validateWeight(finalLoad))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			player.sendActionFailed();
			return;
		}

		if(!player.getInventory().validateCapacity(finalCount))
		{
			sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			player.sendActionFailed();
			return;
		}

		if(player.reduceAdena("Buy", totalCost, merchant, false))
		{
			for(int i = 0; i < _count; i++)
			{
				itemId = (int) _items[i * 2];
				cnt = _items[i * 2 + 1];

				TradeItem ti = sellList.getTradeItem(itemId);

				if(ti.getLimitCount() > 0)
				{
					synchronized(ti)
					{
						if(ti.getCount() < cnt)
						{
							player.sendActionFailed();
							return;
						}

						ti.setCount(ti.getCount() - cnt);
						ti.setLimitResetTime(System.currentTimeMillis() + 3600000);
					}
				}
				player.getInventory().addItem("Buy", itemId, cnt, player, merchant);
			}

			// Add tax to castle treasury if not owned by npc clan
			if(merchant != null && merchant.getCastle().getOwnerId() > 0)
			{
				merchant.getCastle().addToTreasury((int) tax, true, false, "BUY_ITEM");
			}
		}
		//sendPacket(new ItemList(player, true));
		player.sendChanges();
		sendPacket(new ExBuyList(sellList, player, taxRate));
		sendPacket(new ExSellRefundList(player).done());
		//player.doInteract(merchant);
	}
}