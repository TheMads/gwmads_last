package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public class GMViewItemList extends AbstractItemPacket
{
	private L2ItemInstance[] _items;
	private L2Player _player;

	public GMViewItemList(L2Player player)
	{
		_items = player.getInventory().getItems();
		_player = player;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9A);
		writeS(_player.getName());
		writeD(_player.getInventoryLimit()); //c4?
		writeH(1); // show window ??

		writeH(_items.length);

		for(L2ItemInstance temp : _items)
		{
			writeItemInfo(temp);
		}
	}
}