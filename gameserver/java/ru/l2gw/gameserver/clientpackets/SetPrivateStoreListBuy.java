package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.SafeMath;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2TradeList;
import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.ChangeWaitType;
import ru.l2gw.gameserver.serverpackets.PrivateStoreManageListBuy;
import ru.l2gw.gameserver.serverpackets.PrivateStoreMsgBuy;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.util.Util;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SetPrivateStoreListBuy extends L2GameClientPacket
{
	// format: cdb, b - array of (dhhdd)
	private int _count;
	private long[] _items; // count * 3

	@Override
	public void readImpl()
	{
		_count = readD();
		if(_count * 40 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}

		_items = new long[_count * 5];
		for(int i = 0; i < _count; i++)
		{
			int itemId = readD();
			_items[i * 5 + 0] = Util.pack2Long(itemId, Util.pack2Int((short) readH(), (short) readH())); //item id, enchant
			_items[i * 5 + 1] = readQ(); //count
			_items[i * 5 + 2] = readQ(); //price
			if(itemId < 1 || _items[i * 5 + 1] < 1)
			{
				_items = null;
				break;
			}

			// Gracia Final
			// Attack Element
			// Attack value
			// DefAttr Fire
			// DefAttr Water
			_items[i * 5 + 3] = Util.pack2Long((short) readH(), (short) readH(), (short) readH(), (short) readH());
			// DefAttr Wind
			// DefAttr Earth
			// DefAttr Holy
			// DefAttr Dark;
			_items[i * 5 + 4] = Util.pack2Long((short) readH(), (short) readH(), (short) readH(), (short) readH());
		}
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(_items == null)
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
		ConcurrentLinkedQueue<TradeItem> listbuy = new ConcurrentLinkedQueue<TradeItem>();
		long totalCost = 0;

		for(int i = 0; i < _count; i++)
		{
			int[] unpack = Util.unpack2Int(_items[i * 5]);
			int itemId = unpack[0];
			int enchant = Util.unpack2Short(unpack[1])[0];

			if(_items[i * 5 + 1] < 1 || player.getInventory().getItemByItemId(itemId) == null)
				continue;

			try
			{
				long cost = SafeMath.safeMulLong(_items[i * 5 + 1], _items[i * 5 + 2]);
				totalCost = SafeMath.safeAddLong(cost, totalCost);
			}
			catch(ArithmeticException e)
			{
				player.sendPacket(Msg.INCORRECT_ITEM_PRICE);
				abortStore(player);
				return;
			}

			temp = new TradeItem();
			temp.setItemId(itemId);
			temp.setEnchantLevel(enchant);
			temp.setCount(_items[i * 5 + 1]);
			temp.setOwnersPrice(_items[i * 5 + 2]);
			short[] attr = Util.unpack2Short(_items[i * 5 + 3]);
			temp.setAttackElement(new int[]{ attr[0], attr[1]});
			temp.setDefenceFire(attr[2]);
			temp.setDefenceWater(attr[3]);

			attr = Util.unpack2Short(_items[i * 5 + 4]);
			temp.setDefenceWind(attr[0]);
			temp.setDefenceEarth(attr[1]);
			temp.setDefenceHoly(attr[2]);
			temp.setDefenceUnholy(attr[3]);

			if(temp.getOwnersPrice() < 0 || temp.getCount() < 0)
			{
				cancelStore(player);
				return;
			}
			if(totalCost > player.getAdena())
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_PURCHASE_PRICE_IS_HIGHER_THAN_THE_AMOUNT_OF_MONEY_THAT_YOU_HAVE_AND_SO_YOU_CANNOT_OPEN_A_PERSONAL_STORE));
				abortStore(player);
				return;
			}
			listbuy.add(temp);
		}

		if(listbuy.size() > 0)
		{
			player.setBuyList(listbuy);
			player.setPrivateStoreType(L2Player.STORE_PRIVATE_BUY);
			player.broadcastPacket(new ChangeWaitType(player, ChangeWaitType.WT_SITTING));
			player.broadcastUserInfo(true);
			player.broadcastPacket(new PrivateStoreMsgBuy(player));
			player.setPrivateStoreManage(false);
			player.sitDown();
			return;
		}

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
		player.sendPacket(new PrivateStoreManageListBuy(player));
	}
}