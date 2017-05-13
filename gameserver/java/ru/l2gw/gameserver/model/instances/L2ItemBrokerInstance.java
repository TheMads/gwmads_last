package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ItemAuctionManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.itemauction.ItemAuction;
import ru.l2gw.gameserver.serverpackets.ConfirmDlg;
import ru.l2gw.gameserver.serverpackets.ExItemAuctionInfo;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * @author: rage
 * @date: 28.08.2010 20:31:22
 */
public class L2ItemBrokerInstance extends L2NpcInstance
{
	public L2ItemBrokerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.startsWith("ItemAuctionBid"))
		{
			ItemAuction ia = ItemAuctionManager.getInstance().getAuctionByBrokerId(getNpcId());
			if(ia != null)
			{
				player.setLastNpc(this);
				player.sendPacket(new ExItemAuctionInfo(ia, false));
			}
			else
				showChatWindow(player, 2);
		}
		else if(command.startsWith("ItemAuctionWithdraw"))
		{
			ItemAuction ia = ItemAuctionManager.getInstance().getAuctionByBrokerId(getNpcId());
			if(ia != null)
			{
				String var = player.getVar("bid-" + getNpcId());
				long bid = Long.parseLong(var != null && !var.isEmpty() ? var : "0");
				if(bid == 0)
					player.sendPacket(Msg.THERE_ARE_NO_FUNDS_PRESENTLY_DUE_TO_YOU);
				else
					player.sendPacket(new ConfirmDlg(SystemMessage.THE_BID_AMOUNT_WAS_S1_ADENA_WOULD_YOU_LIKE_TO_RETRIEVE_THE_BID_AMOUNT, 0, 65).addNumber(bid));
			}
			else
				showChatWindow(player, 2);
		}
		else
			super.onBypassFeedback(player, command);
	}
}
