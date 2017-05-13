package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2TradeList;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SendTradeDone;
import ru.l2gw.gameserver.serverpackets.TradeOtherAdd;
import ru.l2gw.gameserver.serverpackets.TradeOwnAdd;
import ru.l2gw.gameserver.serverpackets.TradeUpdate;

public class AddTradeItem extends L2GameClientPacket
{
	//Format: cddd
	@SuppressWarnings("unused")
	private int _tradeId;
	private int _objectId;
	private long _amount;

	@Override
	public void readImpl()
	{
		_tradeId = readD();
		_objectId = readD();
		_amount = readQ();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || _amount < 1)
			return;
		L2Player requestor = player.getTransactionRequester();

		L2TradeList playerItemList = player.getTradeList();

		if(requestor == null || requestor.getTransactionRequester() == null)
		{
			// trade partner logged off. trade is canceld
			player.sendPacket(new SendTradeDone(0));
			player.sendPacket(Msg.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.setTransactionRequester(null);
			if(playerItemList != null && playerItemList.getItems() != null)
				playerItemList.getItems().clear();
			return;
		}

		if(requestor.getTradeList() == null || playerItemList == null)
		{
			player.sendPacket(Msg.SYSTEM_ERROR);
			player.sendActionFailed();
			return;
		}

		if(requestor.getTradeList().hasConfirmed() || playerItemList.hasConfirmed())
		{
			player.sendPacket(Msg.YOU_CANNOT_MOVE_ADDITIONAL_ITEMS_BECAUSE_TRADE_HAS_BEEN_CONFIRMED);
			player.sendActionFailed();
			return;
		}

		L2ItemInstance InvItem = player.getInventory().getItemByObjectId(_objectId);

		if(InvItem == null || !InvItem.canBeTraded(player))
		{
			player.sendPacket(Msg.THIS_ITEM_CANNOT_BE_TRADED_OR_SOLD);
			return;
		}

		long InvItemCount = InvItem.getCount();

		L2ItemInstance TradeItem;

		long realCount = Math.min(_amount, InvItemCount);
		long leaveCount = InvItemCount - realCount;

		if(playerItemList.getItems().size() <= 0 || !playerItemList.contains(_objectId))
		{
			TradeItem = new L2ItemInstance(_objectId, InvItem.getItem());
			TradeItem.setCount(realCount);
			TradeItem.setEnchantLevel(InvItem.getEnchantLevel());
			playerItemList.addItem(TradeItem);
		}
		else
		{
			TradeItem = playerItemList.getItem(_objectId);
			if(TradeItem == null)
				return;
			long TradeItemCount = TradeItem.getCount();
			if(InvItemCount == TradeItemCount)
				return;

			if(_amount + TradeItemCount >= InvItemCount)
				realCount = InvItemCount - TradeItemCount;

			TradeItem.setCount(realCount + TradeItemCount);
			leaveCount = InvItemCount - realCount - TradeItemCount;
			realCount += TradeItemCount;
		}

		player.sendPacket(new TradeOwnAdd(InvItem, realCount));
		player.sendPacket(new TradeUpdate(InvItem, leaveCount));
		requestor.sendPacket(new TradeOtherAdd(InvItem, realCount));
	}
}