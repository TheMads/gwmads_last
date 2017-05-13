package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.util.Util;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Список продаваемого в приватный магазин покупки
 */
public class SendPrivateStoreBuyBuyList extends L2GameClientPacket
{
	// format: cddb, b - array of (ddhhdd)
	private int buyerID;
	private L2Player buyer;
	private L2Player seller;
	private int count;
	private long sumPrice = 0;
	private ConcurrentLinkedQueue<TradeItem> sellerlist = new ConcurrentLinkedQueue<TradeItem>();
	private ConcurrentLinkedQueue<TradeItem> buyerlist = new ConcurrentLinkedQueue<TradeItem>();

	private boolean _fail = false;
	private boolean seller_fail = false;

	@Override
	public void readImpl()
	{
		GameClient client = getClient();
		buyerID = readD();
		seller = client.getPlayer();
		buyer = (L2Player) seller.getVisibleObject(buyerID);
		count = readD();

		if(count * 28 > _buf.remaining() || count > Short.MAX_VALUE || count <= 0)
		{
			seller_fail = true;
			return;
		}

		if(seller == null || buyer == null || buyer.getTradeList() == null)
		{
			_fail = true;
			return;
		}

		if(!seller.isInRange(buyer, seller.getInteractDistance(buyer)))
		{
			seller_fail = true;
			return;
		}

		if(AdminTemplateManager.checkBoolean("noPrivateStore", seller))
		{
			seller.sendPacket(new SystemMessage(SystemMessage.THIS_ACCOUNT_CANOT_USE_PRIVATE_STORES));
			_fail = true;
			return;
		}

		buyerlist = buyer.getBuyList();

		long totalPrice = 0;
		long totalCount = 0;

		TradeItem temp;
		for(int i = 0; i < count; i++)
		{
			temp = new TradeItem();

			temp.setObjectId(readD()); // ObjectId работает просто клиенту нужно слать корректные данные из PrivateStoreListBuy

			temp.setItemId(readD());
			readH();
			readH();
			long itemcount = readQ();
			temp.setOwnersPrice(readQ());

			if(temp.getItemId() < 1 || itemcount < 1 || temp.getOwnersPrice() < 1)
			{
				seller.sendPacket(Msg.ActionFail);
				_log.info("temp: itemId: " + temp.getItemId() + " count: " + itemcount + " price: " + temp.getOwnersPrice());
				seller.sendPacket(new SystemMessage(SystemMessage.THE_ATTEMPT_TO_SELL_HAS_FAILED));
				_fail = true;
				return;
			}

			L2ItemInstance SIItem = seller.getInventory().getItemByObjectId(temp.getObjectId());

			if(SIItem == null)
			{
				// if(Config.DEBUG)
				_log.warn("Player " + seller.getName() + " tries to sell to PSB:" + buyer.getName() + " item not in inventory");
				continue;
			}

			if(SIItem.getItemId() != temp.getItemId())
			{
				Util.handleIllegalPlayerAction(seller, seller.getName() + " try sell bug with fake object id. Item: " + SIItem + " try to sell as itemId: " + temp.getItemId() + " buyer: " + buyer, "", Config.DEFAULT_PUNISH);
				seller.sendPacket(new SystemMessage(SystemMessage.THE_ATTEMPT_TO_SELL_HAS_FAILED));
				_fail = true;
				return;
			}

			if(SIItem.isEquipped() || !SIItem.canBeTraded(seller))
			{
				_log.info("sell item: " + SIItem + " equipped: " + SIItem.isEquipped());
				seller.sendPacket(new SystemMessage(SystemMessage.THE_ATTEMPT_TO_SELL_HAS_FAILED));
				_fail = true;
				return;
			}

			//			temp.setObjectId(SIItem.getObjectId());

			if(itemcount > SIItem.getCount())
				itemcount = SIItem.getCount();

			temp.setCount(itemcount);
			temp.setEnchantLevel(SIItem.getEnchantLevel());
			temp.setAttackElement(SIItem.getAttackElement());
			temp.setDefenceFire(SIItem.getDefenceFire());
			temp.setDefenceWater(SIItem.getDefenceWater());
			temp.setDefenceWind(SIItem.getDefenceWind());
			temp.setDefenceEarth(SIItem.getDefenceEarth());
			temp.setDefenceHoly(SIItem.getDefenceHoly());
			temp.setDefenceUnholy(SIItem.getDefenceDark());
			temp.setEnchantOptionId(0, SIItem.getEnchantOptionId(0));
			temp.setEnchantOptionId(1, SIItem.getEnchantOptionId(1));
			temp.setEnchantOptionId(2, SIItem.getEnchantOptionId(2));

			totalPrice += temp.getOwnersPrice() * temp.getCount();
			totalCount += temp.getCount();
			sumPrice += temp.getOwnersPrice() * temp.getCount();

			sellerlist.add(temp);
		}

		if(totalPrice < 0 || totalCount < 0)
		{
			Util.handleIllegalPlayerAction(seller, "SendPrivateStoreBuyBuyList[47]", "tried an overflow exploit totalPrice: " + totalPrice + " totalCount: " + totalCount, 0);
			_fail = true;
			return;
		}

		_fail = false;
	}

	@Override
	public void runImpl()
	{
		if(seller_fail)
		{
			if(seller != null)
				seller.sendActionFailed();
			return;
		}

		if(buyer == null)
		{
			if(seller != null)
				seller.sendActionFailed();
			return;
		}

		if(_fail)
		{
			cancelStore(buyer);
			return;
		}

		if(!GeoEngine.canSeeTarget(buyer, seller))
		{
			seller.sendPacket(Msg.CANNOT_SEE_TARGET);
			return;
		}

		if(buyer.getAdena() < sumPrice || buyer.getPrivateStoreType() != L2Player.STORE_PRIVATE_BUY)
		{
			cancelStore(buyer);
			return;
		}
		buyer.getTradeList().buySellItems(buyer, buyerlist, seller, sellerlist);
		buyer.saveTradeList();

		if(buyer.getBuyList().size() == 0)
			cancelStore(buyer);

		buyer.updateStats();
		seller.sendActionFailed();
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