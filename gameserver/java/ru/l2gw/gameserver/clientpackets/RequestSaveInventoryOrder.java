package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

public class RequestSaveInventoryOrder extends L2GameClientPacket
{
	// format: (ch)db, b - array of (dd)
	int[][] _items;

	@Override
	public void readImpl()
	{
		int size = readD();
		if(size > 125)
			size = 125;
		if(!checkReadArray(size, 8) || size == 0)
		{
			_items = null;
			return;
		}
		_items = new int[size][2];
		for(int i = 0; i < size; i++)
		{
			_items[i][0] = readD(); // item id
			_items[i][1] = readD(); // slot
		}
	}

	@Override
	public void runImpl()
	{
		if(_items == null)
			return;
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		player.getInventory().sort(_items);
	}
}