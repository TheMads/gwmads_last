package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;

public class PetItemList extends AbstractItemPacket
{
	private L2ItemInstance[] items;

	public PetItemList(L2PetInstance cha)
	{
		items = cha.getInventory().getItems();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xB3);
		writeH(items.length);

		for(L2ItemInstance temp : items)
			writeItemInfo(temp);
	}
}