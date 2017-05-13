package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.templates.L2Item;

/**
 * @author rage
 * @date 17.06.2010 16:58:50
 */
public class ExReplyPostItemList extends AbstractItemPacket
{
	private final GArray<L2ItemInstance> _itemList;
	
	public ExReplyPostItemList(L2Player player)
	{
		_itemList = new GArray<>();
		
		for(L2ItemInstance item : player.getInventory().getItems())
			if(!item.isEquipped() && item.getItem().getType2() != L2Item.TYPE2_QUEST && item.canBeTraded(player) && item.getItem().isKeepType(L2Item.KEEP_TYPE_MAIL))
				_itemList.add(item);
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xB3);
		writeD(_itemList.size());
		for(L2ItemInstance temp : _itemList)
			writeItemInfo(temp);
	}
}
