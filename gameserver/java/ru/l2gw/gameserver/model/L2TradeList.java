package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.SafeMath;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;

import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public class L2TradeList
{
	private static Log _log = LogFactory.getLog(L2TradeList.class.getName());

	private final GArray<L2ItemInstance> _items = new GArray<L2ItemInstance>();
	private static final GArray<L2ItemInstance> _emptyList = new GArray<L2ItemInstance>(0); 
	private int _listId;
	private boolean _confirmed;
	private String _buyStoreName, _sellStoreName;

	private String _npcId;

	public L2TradeList(int listId)
	{
		_listId = listId;
		_confirmed = false;
	}

	public L2TradeList()
	{
		this(0);
	}

	public void setNpcId(String id)
	{
		_npcId = id;
	}

	public String getNpcId()
	{
		return _npcId;
	}

	public void addItem(L2ItemInstance item)
	{
		synchronized (_items)
		{
			_items.add(item);
		}
	}

	public void removeAll()
	{
		_items.clear();
	}

	/**
	 * @return Returns the listId.
	 */
	public int getListId()
	{
		return _listId;
	}

	public void setSellStoreName(String name)
	{
		if(name != null && name.length() > 29)
			name = name.substring(0, 29);
		_sellStoreName = name;
	}

	public String getSellStoreName()
	{
		return _sellStoreName;
	}

	public void setBuyStoreName(String name)
	{
		if(name != null && name.length() > 29)
			name = name.substring(0, 29);
		_buyStoreName = name;
	}

	public String getBuyStoreName()
	{
		return _buyStoreName;
	}

	/**
	 * @return Returns the items.
	 */
	public GArray<L2ItemInstance> getItems()
	{
		return _items == null ? _emptyList : _items;
	}

	public long getPriceForItemId(int itemId)
	{
		synchronized (_items)
		{
			for(L2ItemInstance item : _items)
				if(item.getItemId() == itemId)
					return item.getPriceToSell();
		}
		return -1;
	}

	public L2ItemInstance getItemByItemId(int itemId)
	{
		synchronized (_items)
		{
			for(L2ItemInstance item : _items)
				if(item.getItemId() == itemId)
					return item;
		}
		return null;
	}

	public L2ItemInstance getItem(int ObjectId)
	{
		synchronized (_items)
		{
			for(L2ItemInstance item : _items)
				if(item.getObjectId() == ObjectId)
					return item;
		}
		return null;
	}

	public void setConfirmedTrade(boolean x)
	{
		_confirmed = x;
	}

	public boolean hasConfirmed()
	{
		return _confirmed;
	}

	public boolean contains(int objId)
	{
		synchronized (_items)
		{
			for(L2ItemInstance item : _items)
				if(item.getObjectId() == objId)
					return true;
		}
		return false;
	}

	public boolean validateTrade(L2Player player)
	{
		Inventory playersInv = player.getInventory();
		L2ItemInstance playerItem;
		synchronized (_items)
		{
			for(L2ItemInstance item : _items)
			{
				playerItem = playersInv.getItemByObjectId(item.getObjectId());
				if(playerItem == null || playerItem.getCount() < item.getCount() || playerItem.isEquipped())
					return false;
			}
		}
		return true;
	}

	// Call validate before this
	// synchronized не трогать - CME фикс!
	public synchronized void tradeItems(L2Player player, L2Player reciever)
	{
		Inventory playersInv = player.getInventory();
		Inventory recieverInv = reciever.getInventory();

		for(L2ItemInstance temp : _items)
		{
			// If player trades the enchant scroll he was using remove its effect
			if(player.getEnchantScroll() != null && temp.getObjectId() == player.getEnchantScroll().getObjectId())
				player.setEnchantScroll(null);

			L2ItemInstance oldItem = playersInv.getItemByObjectId(temp.getObjectId());
			if(oldItem == null)
				continue;

			player.transferItem("Trade", temp.getObjectId(), temp.getCount(), recieverInv, reciever);
		}

		player.updateStats();
		reciever.updateStats();
	}

	public void updateSellList(L2Player player, ConcurrentLinkedQueue<TradeItem> list)
	{
		Inventory playersInv = player.getInventory();
		L2ItemInstance item;
		for(L2ItemInstance temp : _items)
		{
			item = playersInv.getItemByObjectId(temp.getObjectId());
			if(item == null || item.getCount() <= 0)
			{
				for(TradeItem i : list)
					if(i.getObjectId() == temp.getItemId())
					{
						list.remove(i);
						break;
					}
			}
			else if(item.getCount() < temp.getCount())
				temp.setCount(item.getCount());
		}
	}

	public synchronized void buySellItems(L2Player buyer, ConcurrentLinkedQueue<TradeItem> listToBuy, L2Player seller, ConcurrentLinkedQueue<TradeItem> listToSell)
	{
		Inventory sellerInv = seller.getInventory();
		Inventory buyerInv = buyer.getInventory();

		TradeItem sellerTradeItem = null;
		L2ItemInstance sellerInventoryItem = null;
		L2ItemInstance temp;
		ConcurrentLinkedQueue<TradeItem> unsold = new ConcurrentLinkedQueue<TradeItem>();
		unsold.addAll(listToSell);

		long cost = 0, amount = 0;

		for(TradeItem buyerTradeItem : listToBuy)
		{
			sellerTradeItem = null;

			for(TradeItem unsoldItem : unsold)
				if(unsoldItem.getItemId() == buyerTradeItem.getItemId() && unsoldItem.getOwnersPrice() == buyerTradeItem.getOwnersPrice())
				{
					sellerTradeItem = unsoldItem;
					break;
				}

			if(sellerTradeItem == null)
				continue;

			sellerInventoryItem = sellerInv.getItemByObjectId(sellerTradeItem.getObjectId());

			unsold.remove(sellerTradeItem);

			if(sellerInventoryItem == null)
				continue;

			long buyerItemCount = buyerTradeItem.getCount();
			long sellerItemCount = sellerTradeItem.getCount();

			if(sellerItemCount > sellerInventoryItem.getCount())
				sellerItemCount = sellerInventoryItem.getCount();

			if(seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL || seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE)
			{
				if(buyerItemCount > sellerItemCount)
					buyerTradeItem.setCount(sellerItemCount);
				if(buyerItemCount > sellerInventoryItem.getCount())
					buyerTradeItem.setCount(sellerInventoryItem.getCount());
				buyerItemCount = buyerTradeItem.getCount();
				amount = buyerItemCount;

				try
				{
					cost = SafeMath.safeMulLong(amount, sellerTradeItem.getOwnersPrice());
				}
				catch(ArithmeticException e)
				{
					_log.warn("Overflow on Cost. Possible Exploit attempt between " + buyer.getName() + " and " + seller.getName() + ".");
					_log.warn(seller.getName() + " try to use exploit, ban this player!");
					seller.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.L2TradeList.BuyerExploit", seller));
					return;
				}
			}

			if(buyer.getPrivateStoreType() == L2Player.STORE_PRIVATE_BUY)
			{
				if(sellerItemCount > buyerItemCount)
					sellerTradeItem.setCount(buyerItemCount);
				if(sellerItemCount > sellerInventoryItem.getCount())
					sellerTradeItem.setCount(sellerInventoryItem.getCount());
				sellerItemCount = sellerTradeItem.getCount();
				amount = sellerItemCount;
				try
				{
					cost = SafeMath.safeMulLong(amount, buyerTradeItem.getOwnersPrice());
				}
				catch(ArithmeticException e)
				{
					_log.warn("Overflow on Cost. Possible Exploit attempt between " + buyer.getName() + " and " + seller.getName() + ".");
					_log.warn(buyer.getName() + " try to use exploit, ban this player!");
					seller.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.L2TradeList.BuyerExploit", seller));
					return;
				}
			}

			long sum;
			try
			{
				sum = SafeMath.safeMulLong(buyerItemCount, buyerTradeItem.getOwnersPrice());
			}
			catch(ArithmeticException e)
			{
				_log.warn("Overflow on Cost. Possible Exploit attempt between " + buyer.getName() + " and " + seller.getName() + ".");
				_log.warn(buyer.getName() + " try to use exploit, ban this player!");
				seller.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.L2TradeList.BuyerExploit", seller));
				return;
			}

			try
			{
				sum = SafeMath.safeMulLong(sellerItemCount, sellerTradeItem.getOwnersPrice());
			}
			catch(ArithmeticException e)
			{
				_log.warn("Integer Overflow on Cost. Possible Exploit attempt between " + buyer.getName() + " and " + seller.getName() + ".");
				_log.warn(seller.getName() + " try to use exploit, ban this player!");
				buyer.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.L2TradeList.SellerExploit", buyer));
				return;
			}

			if(buyer.getAdena() < cost)
			{
				_log.warn("buy item without full adena sum " + buyer.getName() + " and " + seller.getName() + ".");
				return;
			}

			if(sellerInventoryItem.canBeTraded(seller) && buyer.reduceAdena("PrivateStore", cost, seller, false))
			{
				seller.addAdena("PrivateStore", cost, buyer, false);
				temp = seller.transferItem("PrivateStore", sellerInventoryItem.getObjectId(), amount, buyerInv, buyer);

				long tax = (long) (cost * Config.SERVICES_TRADE_TAX / 100);
				if(seller.isInZone(L2Zone.ZoneType.offshore))
					tax = (long) (cost * Config.SERVICES_OFFSHORE_TRADE_TAX / 100);
				if(Config.SERVICES_TRADE_TAX_ONLY_OFFLINE && !seller.isInOfflineMode())
					tax = 0;
				if(tax > 0)
				{
					seller.reduceAdena("Tax", tax, null, false);
					L2World.addTax(tax);
					seller.sendMessage(new CustomMessage("trade.HavePaidTax", seller).addNumber(tax));
				}

				if(!temp.isStackable())
				{
					if(temp.getEnchantLevel() > 0)
					{
						seller.sendPacket(new SystemMessage(SystemMessage.C1_PURCHASED_S2_S3).addString(buyer.getName()).addNumber(temp.getEnchantLevel()).addItemName(sellerInventoryItem.getItemId()));
						buyer.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_PURCHASED_S2_S3_FROM_C1).addString(seller.getName()).addNumber(temp.getEnchantLevel()).addItemName(sellerInventoryItem.getItemId()));
					}
					else
					{
						seller.sendPacket(new SystemMessage(SystemMessage.C1_PURCHASED_S2).addString(buyer.getName()).addItemName(sellerInventoryItem.getItemId()));
						buyer.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_PURCHASED_S2_FROM_C1).addString(seller.getName()).addItemName(sellerInventoryItem.getItemId()));
					}
				}
				else
				{
					seller.sendPacket(new SystemMessage(SystemMessage.C1_PURCHASED_S3_S2S).addString(buyer.getName()).addItemName(sellerInventoryItem.getItemId()).addNumber(amount));
					buyer.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_PURCHASED_S3_S2S_FROM_C1).addString(seller.getName()).addItemName(sellerInventoryItem.getItemId()).addNumber(amount));
				}
			}
		}

		seller.sendChanges();
		buyer.sendChanges();

		HashSet<TradeItem> tmp;

		if(seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE)
			seller.setSellList(null);

		// update seller's sell list
		if(seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL)
		{
			tmp = new HashSet<TradeItem>();
			tmp.addAll(seller.getSellList());

			for(TradeItem sl : listToSell)
				for(TradeItem bl : listToBuy)
					if(tmp.contains(sl) && sl.getItemId() == bl.getItemId() && sl.getOwnersPrice() == bl.getOwnersPrice())
					{
						L2ItemInstance inst = seller.getInventory().getItemByObjectId(sl.getObjectId());
						if(inst == null || inst.getCount() <= 0)
						{
							tmp.remove(sl);
							break;
						}
						if(inst.isStackable())
						{
							sl.setCount(sl.getCount() - bl.getCount());
							if(sl.getCount() <= 0)
							{
								tmp.remove(sl);
								break;
							}
							if(inst.getCount() < sl.getCount())
								sl.setCount(inst.getCount());
						}
					}

			ConcurrentLinkedQueue<TradeItem> newlist = new ConcurrentLinkedQueue<TradeItem>();
			newlist.addAll(tmp);
			seller.setSellList(newlist);
		}

		// update buyer's buy list
		if(buyer.getPrivateStoreType() == L2Player.STORE_PRIVATE_BUY)
		{
			tmp = new HashSet<TradeItem>();
			tmp.addAll(buyer.getBuyList());

			for(TradeItem sl : listToSell)
			{
				for(TradeItem bl : listToBuy)
				{
					if(tmp.contains(bl) && sl.getItemId() == bl.getItemId() && sl.getOwnersPrice() == bl.getOwnersPrice())
					{
						if(ItemTable.getInstance().getTemplate(bl.getItemId()).isStackable())
						{
							bl.setCount(bl.getCount() - sl.getCount());
							if(bl.getCount() <= 0)
							{
								tmp.remove(bl);
								break;
							}
						}
						else
						{
							tmp.remove(bl);
							break;
						}
					}
				}
			}

			ConcurrentLinkedQueue<TradeItem> newlist = new ConcurrentLinkedQueue<TradeItem>();
			newlist.addAll(tmp);
			buyer.setBuyList(newlist);
		}
	}

	public ConcurrentLinkedQueue<TradeItem> getAvailableItemsForSell(ConcurrentLinkedQueue<TradeItem> bList, ConcurrentLinkedQueue<L2ItemInstance> sList, L2Player seller)
	{
		ConcurrentLinkedQueue<TradeItem> buyList = new ConcurrentLinkedQueue<TradeItem>();
		ConcurrentLinkedQueue<Integer> addedItems = new ConcurrentLinkedQueue<Integer>();

		for(TradeItem buyItem : bList)
		{
			TradeItem bi = buyItem.clone();

			for(L2ItemInstance sellItem : sList)
			{
				if(!addedItems.contains(sellItem.getObjectId()) && buyItem.equals(sellItem) && sellItem.isAvailable(seller, false) && sellItem.canBeTraded(seller))
				{
					bi.setObjectId(sellItem.getObjectId());
					bi.setTempValue(Math.min(buyItem.getCount(), sellItem.getCount()));
					addedItems.add(sellItem.getObjectId());
					break;
				}
			}
			buyList.add(bi);
		}
		return buyList;
	}
}