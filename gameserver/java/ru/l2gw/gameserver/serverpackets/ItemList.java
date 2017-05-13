package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.L2LockType;
import ru.l2gw.commons.arrays.GArray;

public class ItemList extends AbstractItemPacket
{
	private final GArray<L2ItemInstance> _items;
	private final boolean _showWindow;


	public ItemList(GArray<L2ItemInstance> items, boolean showWindow)
	{
		_items = items;
		_showWindow = showWindow;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x11);
		writeH(_showWindow ? 1 : 0);

		writeH(_items.size());
		for(L2ItemInstance temp : _items)
			writeItemInfo(temp);
		writeH(0x00);
	}
}