package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.instances.*;
import ru.l2gw.gameserver.serverpackets.ExBuyList;
import ru.l2gw.gameserver.serverpackets.ExSellRefundList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.util.Util;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RequestSellItem extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _listId;
	private int _count;
	private long[] _items; // count*3

	@Override
	public void readImpl()
	{
		_listId = readD();
		_count = readD();
		if(_count * 16 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new long[_count * 3];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 3 + 0] = readD();
			_items[i * 3 + 1] = readD();
			_items[i * 3 + 2] = readQ();
			if(_items[i * 3 + 0] < 1 || _items[i * 3 + 1] < 1 || _items[i * 3 + 2] < 1)
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
			return;

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

		L2NpcInstance merchant = player.getLastNpc();

		boolean isValidMerchant = merchant instanceof L2ClanBaseManagerInstance || merchant instanceof L2MerchantInstance || merchant instanceof L2MercManagerInstance || merchant instanceof L2CastleChamberlainInstance;

		if(!player.isGM() && (merchant == null || !isValidMerchant || !player.isInRange(merchant, player.getInteractDistance(merchant))))
		{
			player.sendActionFailed();
			return;
		}

		for(int i = 0; i < _count; i++)
		{
			int objectId = (int) _items[i * 3 + 0];
			int itemId = (int) _items[i * 3 + 1];
			long cnt = _items[i * 3 + 2];

			if(cnt < 0)
			{
				Util.handleIllegalPlayerAction(player, "Integer overflow", "RequestSellItem[100]", 0);
				continue;
			}
			else if(cnt == 0)
				continue;

			L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
			if(item == null || !item.canBeSelled(player) || item.isEquipped())
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_ATTEMPT_TO_SELL_HAS_FAILED));
				return;
			}

			if(item.getItemId() != itemId)
			{
				Util.handleIllegalPlayerAction(player, "Fake packet", "RequestSellItem[115]", 0);
				continue;
			}

			if(item.getCount() < cnt)
			{
				Util.handleIllegalPlayerAction(player, "Incorrect item count", "RequestSellItem[121]", 0);
				continue;
			}

			long price = TradeController.getInstance().getBuyPrice(item.getItemId());
			if(price < 0)
				price = item.getReferencePrice() * cnt / 2;

			// If player sells the enchant scroll he is using, deactivate it
			AbstractEnchantPacket.checkAndCancelEnchant(player);

			L2ItemInstance refund = player.getInventory().dropItem("Sell", item.getObjectId(), cnt, player, merchant);
			player.addAdena("Sell", price, merchant, false);

			ConcurrentLinkedQueue<L2ItemInstance> refundlist = player.getInventory().getRefundItemsList();
			if(refund.isStackable())
			{
				boolean found = false;
				for(L2ItemInstance refItem : refundlist)
					if(refItem.getItemId() == refund.getItemId())
					{
						refItem.changeCount("RefundItem", refund.getCount(), player, merchant);
						found = true;
						break;
					}
				if(!found)
					refundlist.add(refund);
			}
			else
				refundlist.add(refund);

			if(refundlist.size() > 12)
				refundlist.poll();
		}

		double taxRate = 0;
		Castle castle;
		if(merchant != null)
		{
			castle = merchant.getCastle();
			if(castle != null)
				taxRate = castle.getTaxRate();
		}

		NpcTradeList list = TradeController.getInstance().getSellList(_listId);
		player.updateStats();
		sendPacket(new ExBuyList(list, player, taxRate));
		sendPacket(new ExSellRefundList(player).done());
		//player.sendPacket(new ExSellRefundList(TradeController.getInstance().getSellList(_listId), player, taxRate).done());
	}
}