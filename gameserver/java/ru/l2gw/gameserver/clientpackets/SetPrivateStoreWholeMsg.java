package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2TradeList;

/**
 * User: rage
 * Date: 12.11.2008
 * Time: 16:39:25
 */
public class SetPrivateStoreWholeMsg extends L2GameClientPacket
{
	private String _storename;

	@Override
	public void readImpl()
	{
		_storename = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2TradeList tradeList = player.getTradeList();
		if(tradeList != null)
			tradeList.setSellStoreName(_storename);
	}
}
