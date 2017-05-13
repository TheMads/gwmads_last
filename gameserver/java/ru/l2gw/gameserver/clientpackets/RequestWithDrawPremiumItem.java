package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.PremiumItemManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PremiumItem;
import ru.l2gw.gameserver.serverpackets.ExGetPremiumItemList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.util.Util;

/**
 * @author rage
 * @date 17.12.10 0:22
 */
public class RequestWithDrawPremiumItem extends L2GameClientPacket
{
	private int id;
	private int ownerId;
	private long amount;
	
	@Override
	protected void readImpl()
	{
		id = readD();
		ownerId = readD();
		amount = readQ();
	}
	
	@Override
	protected void runImpl()
	{
		final L2Player player = getClient().getPlayer();
		
		if(player == null || amount <= 0)
			return;

		if(player.getObjectId() != ownerId)
		{
			Util.handleIllegalPlayerAction(player, "[RequestWithDrawPremiumItem] Incorrect owner, Player: ", player.getName(), Config.DEFAULT_PUNISH);
			return;
		}

		if(!player.isQuestContinuationPossible(false))
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_RECEIVE_THE_VITAMIN_ITEM));
			return;
		}

		if(player.isTransactionInProgress())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_RECEIVE_A_VITAMIN_ITEM_DURING_AN_EXCHANGE));
			return;
		}

		PremiumItem item = PremiumItemManager.getPremiumItem(ownerId, id);
		if(PremiumItemManager.withdrawItem(player.getObjectId(), id, amount))
			player.addItem("PremiumItem", item.getItemId(), amount, player.getLastNpc(), true);
		
		if(PremiumItemManager.getItemsByObjectId(player.getObjectId()).isEmpty())
			player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_MORE_VITAMIN_ITEMS_TO_BE_FOUND));
		else
			player.sendPacket(new ExGetPremiumItemList(player.getObjectId()));
	}
}
