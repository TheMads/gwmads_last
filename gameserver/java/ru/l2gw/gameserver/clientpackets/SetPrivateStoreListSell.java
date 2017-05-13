package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.SafeMath;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2TradeList;
import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.ExPrivateStoreSetWholeMsg;
import ru.l2gw.gameserver.serverpackets.PrivateStoreManageListSell;
import ru.l2gw.gameserver.serverpackets.PrivateStoreMsgSell;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2Item;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Это список вещей которые игрок хочет продать в создаваемом им приватном магазине
 *
 * Старое название SetPrivateStoreListSell
 */
public class SetPrivateStoreListSell extends L2GameClientPacket
{
	//Format: cddb, b = array of (ddd)
	private int _count;
	@SuppressWarnings("unused")
	private boolean _package;
	private long[] _items; // count * 3

	@Override
	public void readImpl()
	{
		_package = readD() == 1;
		_count = readD();
		// Иначе нехватит памяти при создании массива.
		if(_count * 20 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new long[_count * 3];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 3 + 0] = readD(); //objectId
			_items[i * 3 + 1] = readQ(); //count
			_items[i * 3 + 2] = readQ(); //price
			if(_items[i * 3 + 0] < 1 || _items[i * 3 + 1] < 1 || _items[i * 3 + 2] < 0)
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

		if(_items == null || _count <= 0)
		{
			cancelStore(player);
			return;
		}

		if(player.getMountEngine().isMounted())
		{
			cancelStore(player);
			return;
		}

		int maxSlots = player.getTradeLimit();
		if(_count > maxSlots)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED));
			cancelStore(player);
			return;
		}

		if(player.isActionBlocked(L2Zone.BLOCKED_ACTION_PRIVATE_STORE))
		{
			cancelStore(player);
			return;
		}

		if(Config.ALT_MIN_PRIVATE_STORE_RADIUS > 0)
			for(L2Player cha : player.getAroundPlayers(Config.ALT_MIN_PRIVATE_STORE_RADIUS))
				if(cha.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
				{
					player.sendPacket(Msg.A_PRIVATE_STORE_MAY_NOT_BE_OPENED_IN_THIS_AREA);
					return;
				}


		TradeItem temp;
		ConcurrentLinkedQueue<TradeItem> listsell = new ConcurrentLinkedQueue<TradeItem>();

		int count = _count;
		long totalCost = 0;
		for(int x = 0; x < _count; x++)
		{
			int objectId = (int) _items[x * 3 + 0];
			long cnt = _items[x * 3 + 1];
			long price = _items[x * 3 + 2];

			L2ItemInstance itemToSell = player.getInventory().getItemByObjectId(objectId);

			if(cnt < 1 || itemToSell == null || !itemToSell.canBeTraded(player))
			{
				count--;
				continue;
			}

			// If player sells the enchant scroll he is using, deactivate it
			AbstractEnchantPacket.checkAndCancelEnchant(player);

			if(cnt > itemToSell.getCount())
				cnt = itemToSell.getCount();

			try
			{
				long cost = SafeMath.safeMulLong(cnt, price);
				totalCost = SafeMath.safeAddLong(cost, totalCost);
				if(SafeMath.safeAddLong(totalCost, player.getAdena()) > L2Item.MAX_COUNT)
					throw new ArithmeticException();
			}
			catch(ArithmeticException e)
			{
				player.sendPacket(Msg.INCORRECT_ITEM_PRICE);
				abortStore(player);
				return;
			}

			temp = new TradeItem(itemToSell);
			temp.setCount(cnt);
			temp.setOwnersPrice(price);
			listsell.add(temp);
		}

		if(count != 0)
		{
			player.setSellList(listsell);
			player.setPrivateStoreType(_package ? L2Player.STORE_PRIVATE_SELL_PACKAGE : L2Player.STORE_PRIVATE_SELL);
			player.broadcastUserInfo(true);
			player.broadcastPacket(_package ? new ExPrivateStoreSetWholeMsg(player) : new PrivateStoreMsgSell(player));
			player.setPrivateStoreManage(false);
			player.sitDown();
		}
		else
			cancelStore(player);
	}

	private static void cancelStore(L2Player player)
	{
		player.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		player.broadcastUserInfo(true);
		player.getBuyList().clear();
		if(player.isInOfflineMode() && Config.SERVICES_OFFLINE_TRADE_KICK_NOT_TRADING)
		{
			player.setOfflineMode(false);
			player.logout(false, false, true);
			player.getNetConnection().disconnectOffline();
		}
	}

	private static void abortStore(L2Player player)
	{
		player.setTradeList(new L2TradeList(0));
		player.getTradeList().updateSellList(player, player.getSellList());
		player.sendPacket(new PrivateStoreManageListSell(player, false));
	}
}