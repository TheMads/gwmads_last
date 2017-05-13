package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ItemAuctionManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.itemauction.ItemAuction;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.templates.L2Item;

/**
 * @author: rage
 * @date: 28.08.2010 21:26:16
 */
public class RequestBidItemAuction extends L2GameClientPacket
{
	private int _auctionId;
	private long _bid;

	@Override
	protected void readImpl()
	{
		_auctionId = readD();
		_bid = readQ();
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		ItemAuction ia = ItemAuctionManager.getInstance().getAuctionById(_auctionId);
		if(ia == null)
		{
			player.sendActionFailed();
			return;
		}

		L2NpcInstance broker = player.getLastNpc();

		if(broker == null || broker.getNpcId() != ia.getBrokerId() || !player.isInRange(player.getLastNpc(), player.getInteractDistance(player.getLastNpc())))
		{
			player.sendPacket(Msg.IT_S_TOO_FAR_FROM_THE_NPC_TO_WORK);
			return;
		}

		if(!ia.isStarted())
		{
			player.sendPacket(Msg.IT_IS_NOT_AN_AUCTION_PERIOD);
			return;
		}

		if(player.getObjectId() == ia.getCurrentBidderId())
		{
			player.sendPacket(Msg.YOU_CURRENTLY_HAVE_THE_HIGHEST_BID_BUT_THE_RESERVE_HAS_NOT_BEEN_MET);
			return;
		}

		if(player.getAdena() < _bid)
		{
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA_FOR_THIS_BID);
			return;
		}

		if(_bid > L2Item.MAX_COUNT)
		{
			player.sendPacket(Msg.BIDDING_IS_NOT_ALLOWED_BECAUSE_THE_MAXIMUM_BIDDING_PRICE_EXCEEDS_100_BILLION);
			return;
		}

		player.sendPacket(ia.setBid(player, _bid));
	}
}