package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.ItemAuctionManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.itemauction.ItemAuction;
import ru.l2gw.gameserver.serverpackets.ExItemAuctionInfo;

/**
 * @author: rage
 * @date: 28.08.2010 21:01:21
 */
public class RequestInfoItemAuction extends L2GameClientPacket
{
	private int _auctionId;

	@Override
	protected void readImpl()
	{
		_auctionId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		ItemAuction ia = ItemAuctionManager.getInstance().getAuctionById(_auctionId);
		if(ia != null)
			player.sendPacket(new ExItemAuctionInfo(ia, true));
	}
}