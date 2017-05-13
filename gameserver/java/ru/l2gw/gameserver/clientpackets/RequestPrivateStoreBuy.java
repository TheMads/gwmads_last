package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2Item;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RequestPrivateStoreBuy extends L2GameClientPacket
{
	// format: cddb, b - array of (ddd)
	private int _sellerID;
	private int _count;
	private long[] _items; // count * 3

	@Override
	public void readImpl()
	{
		_sellerID = readD();
		_count = readD();
		if(_count * 20 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new long[_count * 3];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 3 + 0] = readD(); //object id
			_items[i * 3 + 1] = readQ(); //count
			_items[i * 3 + 2] = readQ(); //price
			if(_items[i * 3 + 1] < 0)
			{
				_items = null;
				break;
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void runImpl()
	{
		if(_items == null)
			return;

		L2Player buyer = getClient().getPlayer();
		if(buyer == null)
			return;

		if(AdminTemplateManager.checkBoolean("noPrivateStore", buyer))
		{
			buyer.sendPacket(new SystemMessage(SystemMessage.THIS_ACCOUNT_CANOT_USE_PRIVATE_STORES));
			return;
		}

		ConcurrentLinkedQueue<TradeItem> buyerlist = new ConcurrentLinkedQueue<TradeItem>();

		L2Player seller = (L2Player) buyer.getVisibleObject(_sellerID);

		if(seller == null || seller.getPrivateStoreType() != L2Player.STORE_PRIVATE_SELL && seller.getPrivateStoreType() != L2Player.STORE_PRIVATE_SELL_PACKAGE || !buyer.isInRange(seller, buyer.getInteractDistance(seller)))
		{
			buyer.sendActionFailed();
			return;
		}

		if(seller.getTradeList() == null)
		{
			cancelStore(seller);
			return;
		}

		if(!GeoEngine.canSeeTarget(buyer, seller))
		{
			buyer.sendPacket(Msg.CANNOT_SEE_TARGET);
			return;
		}

		ConcurrentLinkedQueue<TradeItem> sellerlist = seller.getSellList();
		long cost = 0;

		if(seller.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE)
		{
			buyerlist = new ConcurrentLinkedQueue<TradeItem>();
			buyerlist.addAll(sellerlist);
			for(TradeItem ti : buyerlist)
			{
				L2ItemInstance sellerItem = seller.getInventory().getItemByObjectId(ti.getObjectId());
				if(sellerItem == null || sellerItem.getLocation() != L2ItemInstance.ItemLocation.INVENTORY)
				{
					buyer.sendActionFailed();
					return;
				}
				cost += ti.getOwnersPrice() * ti.getCount();
			}
		}
		else
			for(int i = 0; i < _count; i++)
			{
				int objectId = (int)_items[i * 3 + 0];
				long count = _items[i * 3 + 1];
				long price = _items[i * 3 + 2];

				for(TradeItem si : sellerlist)
					if(si.getObjectId() == objectId)
					{
						if(count > si.getCount() || price != si.getOwnersPrice())
						{
							buyer.sendActionFailed();
							return;
						}

						L2ItemInstance sellerItem = seller.getInventory().getItemByObjectId(objectId);
						if(sellerItem == null || sellerItem.getCount() < count || sellerItem.getLocation() != L2ItemInstance.ItemLocation.INVENTORY)
						{
							buyer.sendActionFailed();
							return;
						}

						TradeItem temp = new TradeItem();
						temp.setObjectId(si.getObjectId());
						temp.setItemId(sellerItem.getItemId());
						temp.setCount(count);
						temp.setOwnersPrice(si.getOwnersPrice());
						temp.setAttackElement(sellerItem.getAttackElement());
						temp.setDefenceFire(sellerItem.getDefenceFire());
						temp.setDefenceWater(sellerItem.getDefenceWater());
						temp.setDefenceWind(sellerItem.getDefenceWind());
						temp.setDefenceEarth(sellerItem.getDefenceEarth());
						temp.setDefenceHoly(sellerItem.getDefenceHoly());
						temp.setDefenceUnholy(sellerItem.getDefenceDark());
						temp.setEnchantOptionId(0, sellerItem.getEnchantOptionId(0));
						temp.setEnchantOptionId(1, sellerItem.getEnchantOptionId(1));
						temp.setEnchantOptionId(2, sellerItem.getEnchantOptionId(2));

						cost += temp.getOwnersPrice() * temp.getCount();
						buyerlist.add(temp);
					}
			}

		if(buyer.getAdena() < cost || cost > L2Item.MAX_COUNT || cost < 0)
		{
			buyer.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			buyer.sendActionFailed();
			return;
		}

		seller.getTradeList().buySellItems(buyer, buyerlist, seller, sellerlist);
		buyer.sendChanges();

		seller.saveTradeList();

		if(seller.getSellList().isEmpty())
			cancelStore(seller);

		seller.sendChanges();
		buyer.sendActionFailed();
	}

	private void cancelStore(L2Player player)
	{
		player.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		player.broadcastUserInfo(true);
		player.getBuyList().clear();
		if(player.isInOfflineMode() && Config.SERVICES_OFFLINE_TRADE_KICK_NOT_TRADING)
		{
			player.setOfflineMode(false);
			player.logout(false, false, true);
			if(player.getNetConnection() != null)
				player.getNetConnection().disconnectOffline();
		}
	}
}