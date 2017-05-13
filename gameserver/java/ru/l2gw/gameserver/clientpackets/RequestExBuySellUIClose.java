package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ItemList;

public class RequestExBuySellUIClose extends L2GameClientPacket
{
	@Override
	public void runImpl()
	{
	// trigger
	}

	@Override
	public void readImpl()
	{
		L2Player activeChar = getClient().getPlayer();
		if(activeChar == null || activeChar.isInventoryDisabled())
			return;

		activeChar.setBuyListId(0);
		activeChar.getInventory().sendItemList(true);
	}
}