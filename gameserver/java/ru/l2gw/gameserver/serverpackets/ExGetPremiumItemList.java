package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.instancemanager.PremiumItemManager;
import ru.l2gw.gameserver.model.PremiumItem;

import java.util.List;

/**
 * @author rage
 * @date 16.12.10 16:49
 */
public class ExGetPremiumItemList extends L2GameServerPacket
{
	private List<PremiumItem> items;
	
	public ExGetPremiumItemList(int objectId)
	{
		items = PremiumItemManager.getItemsByObjectId(objectId);
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x87);
		writeD(items.size());
		for(PremiumItem item : items)
		{
			writeD(item.getId());
			writeD(item.getOwnerId());
			writeD(item.getItemId());
			writeQ(item.getCount());
			writeD(0);
			writeS(item.getSender());
		}
	}
}