package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2TradeList;
import ru.l2gw.gameserver.serverpackets.SendTradeDone;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.GmListTable;

/**
 * This class ...
 *
 * @version $Revision: 1.6.2.2.2.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class TradeDone extends L2GameClientPacket
{
	//Format: cd
	private static org.apache.commons.logging.Log _log = LogFactory.getLog(TradeDone.class.getName());

	private int _response;

	@Override
	public void readImpl()
	{
		_response = readD();
	}

	@Override
	public void runImpl()
	{
		synchronized (getClient())
		{
			L2Player player = getClient().getPlayer();
			if(player == null)
				return;

			L2Player requestor = player.getTransactionRequester();

			if(requestor == null || requestor == player)
			{
				player.sendPacket(new SendTradeDone(0));
				player.setTradeList(null);
				player.setTransactionRequester(null);
				player.sendActionFailed();
				return;
			}

			if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || requestor.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
			{
				player.sendPacket(new SendTradeDone(0));
				player.setTradeList(null);
				player.setTransactionRequester(null);
				player.sendActionFailed();
				player.sendPacket(Msg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
				requestor.sendPacket(Msg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
				return;
			}

			L2Player requestor_partner = requestor.getTransactionRequester();

			L2TradeList player_list = player.getTradeList();
			L2TradeList requestor_list = requestor.getTradeList();

			if(requestor_partner != null && requestor_partner == player && player_list != null && requestor_list != null)
			{
				if(_response == 1)
				{
					if(!player_list.hasConfirmed())
					{
						// first party accepted the trade
						requestor.sendPacket(new SystemMessage(SystemMessage.S1_CONFIRMED_TRADE).addString(player.getName()));
						player.sendActionFailed();
						player_list.setConfirmedTrade(true);

						//notify clients that "OK" button has been pressed.
						player.sendPacket(Msg.TradePressOwnOk);
						requestor.sendPacket(Msg.TradePressOtherOk);
					}

					//Check for dual confirmation
					if(!requestor_list.hasConfirmed())
					{
						player.sendActionFailed();
						return;
					}

					//Can't exchange on a big distance
					if(!player.isInRange(requestor, 300) || !GeoEngine.canSeeTarget(player, requestor))
					{
						player.sendPacket(new SendTradeDone(0));
						player.sendPacket(new SystemMessage(SystemMessage.S1_CANCELED_THE_TRADE).addString(requestor.getName()));
						requestor.sendPacket(new SendTradeDone(0));
						requestor.sendPacket(new SystemMessage(SystemMessage.S1_CANCELED_THE_TRADE).addString(player.getName()));

						player.setTradeList(null);
						requestor.setTradeList(null);
						requestor.setTransactionRequester(null);
						player.setTransactionRequester(null);
						return;
					}

					boolean trade1Valid = player.getInventory().validateCapacity(requestor_list.getItems());
					boolean trade2Valid = requestor.getInventory().validateCapacity(player_list.getItems());

					if(!trade1Valid || !trade2Valid)
					{
						player.sendPacket(new SendTradeDone(0));
						if(!trade1Valid)
							player.sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
						else
							player.sendPacket(new SystemMessage(SystemMessage.S1_CANCELED_THE_TRADE).addString(requestor.getName()));

						requestor.sendPacket(new SendTradeDone(0));
						if(!trade2Valid)
							requestor.sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
						else
							requestor.sendPacket(new SystemMessage(SystemMessage.S1_CANCELED_THE_TRADE).addString(player.getName()));

						player.setTradeList(null);
						requestor.setTradeList(null);
						requestor.setTransactionRequester(null);
						player.setTransactionRequester(null);
						return;
					}

					trade1Valid = player_list.validateTrade(player);
					trade2Valid = requestor_list.validateTrade(requestor);
					if(trade1Valid && trade2Valid)
					{
						if(player_list.getItems() == null)
						{
							_log.warn("TradeDone: empty player tradelist?");
							player.sendActionFailed();
							return;
						}

						if(requestor_list.getItems() == null)
						{
							_log.warn("TradeDone: empty requestor tradelist?");
							player.sendActionFailed();
							return;
						}

						player_list.tradeItems(player, requestor);
						requestor_list.tradeItems(requestor, player);
					}
					requestor.sendPacket(new SendTradeDone(1));
					player.sendPacket(new SendTradeDone(1));

					if(trade1Valid && trade2Valid)
					{
						SystemMessage msg = new SystemMessage(SystemMessage.TRADE_HAS_BEEN_SUCCESSFUL);
						requestor.sendPacket(msg);
						player.sendPacket(msg);
					}
					else
					{
						if(!trade2Valid)
						{
							String msgToSend = requestor.getName() + " tried a trade dupe";
							_log.warn(msgToSend);
							GmListTable.broadcastMessageToGMs(msgToSend);
							player.sendPacket(new SystemMessage(SystemMessage.THE_ATTEMPT_TO_TRADE_HAS_FAILED));
							requestor.sendPacket(new SystemMessage(SystemMessage.THE_ATTEMPT_TO_TRADE_HAS_FAILED));
						}

						if(!trade1Valid)
						{
							String msgToSend = player.getName() + " tried a trade dupe";
							_log.warn(msgToSend);
							GmListTable.broadcastMessageToGMs(msgToSend);
							player.sendPacket(new SystemMessage(SystemMessage.THE_ATTEMPT_TO_TRADE_HAS_FAILED));
							requestor.sendPacket(new SystemMessage(SystemMessage.THE_ATTEMPT_TO_TRADE_HAS_FAILED));
						}
					}
				}
				else
				{
					player.sendPacket(new SendTradeDone(0));
					requestor.sendPacket(new SendTradeDone(0));
					requestor.sendPacket(new SystemMessage(SystemMessage.S1_CANCELED_THE_TRADE).addString(player.getName()));
				}

				player.setTradeList(null);
				requestor.setTradeList(null);

				// clear transaction flag
				requestor.setTransactionRequester(null);
				player.setTransactionRequester(null);
			}
			else
			{
				// trade partner logged off. trade is canceled
				player.sendPacket(new SendTradeDone(0));
				player.sendPacket(Msg.TARGET_IS_NOT_FOUND_IN_THE_GAME);
				player.setTransactionRequester(null);
				requestor.setTradeList(null);
				player.setTradeList(null);
			}
		}
	}
}