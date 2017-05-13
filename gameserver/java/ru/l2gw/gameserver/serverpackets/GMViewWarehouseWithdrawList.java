package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public class GMViewWarehouseWithdrawList extends AbstractItemPacket
{
	private final L2ItemInstance[] _items;
	private String _charName;
	private long _charAdena;

	public GMViewWarehouseWithdrawList(L2Player player)
	{
		_charName = player.getName();
		_charAdena = player.getAdena();
		_items = player.getWarehouse().getItems();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9B);
		writeS(_charName);
		writeQ(_charAdena);
		writeH(_items.length);

		for(L2ItemInstance temp : _items)
		{
			writeItemInfo(temp);
			writeD(temp.getObjectId());
		}
	}
}