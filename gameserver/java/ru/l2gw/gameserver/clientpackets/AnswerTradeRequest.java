package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.model.L2TradeList;
import ru.l2gw.gameserver.serverpackets.SendTradeDone;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.serverpackets.TradeStart;
import ru.l2gw.util.Util;

public class AnswerTradeRequest extends L2GameClientPacket
{
	// Format: cd
	private int _response;

	@Override
	public void readImpl()
	{
		_response = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		L2Player requestor = player.getTransactionRequester();

		// trade partner logged off. trade is canceld
		if(requestor == null || requestor.getTransactionRequester() == null)
		{
			if(_response != 0)
			{
				player.sendPacket(new SendTradeDone(0));
				player.sendPacket(Msg.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			}
			player.setTransactionRequester(null);
			player.setTransactionType(TransactionType.NONE);
			return;
		}

		if(requestor.getTransactionRequester() != player)
		{
			player.setTransactionType(TransactionType.NONE);
			player.setTransactionRequester(null);
			Util.handleIllegalPlayerAction(player, "AnswerTradeRequest[49]", "tried to use trade bug with alt+h and " + requestor + " victim " + requestor.getTransactionRequester(), 1);
			return;
		}

		if(player.getTransactionType() != TransactionType.TRADE || player.getTransactionType() != requestor.getTransactionType())
		{
			player.setTransactionRequester(null);
			player.setTransactionType(TransactionType.NONE);
			requestor.setTransactionRequester(null);
			requestor.setTransactionType(TransactionType.NONE);
			return;
		}

		player.fireMethodInvoked(MethodCollection.onTradeStart, new Object[]{requestor});

		if(_response != 1 || player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			requestor.sendPacket(new SystemMessage(SystemMessage.S1_DENIED_YOUR_REQUEST_FOR_TRADE).addString(player.getName()));
			requestor.setTransactionRequester(null);
			requestor.setTransactionType(TransactionType.NONE);
			player.setTransactionRequester(null);
			player.setTransactionType(TransactionType.NONE);
			if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
				player.sendPacket(new SystemMessage(SystemMessage.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM));
			return;
		}

		requestor.sendPacket(new SystemMessage(SystemMessage.BEGIN_TRADING_WITH_S1).addString(player.getName()));
		requestor.sendPacket(new TradeStart(requestor));
		if(requestor.getTradeList() == null)
			requestor.setTradeList(new L2TradeList(0));

		player.sendPacket(new SystemMessage(SystemMessage.BEGIN_TRADING_WITH_S1).addString(requestor.getName()));
		player.sendPacket(new TradeStart(player));
		if(player.getTradeList() == null)
			player.setTradeList(new L2TradeList((short) 0));

		player.setTransactionRequester(requestor);
		requestor.setTransactionRequester(player);
	}
}
