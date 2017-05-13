package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public class ExRpItemLink extends AbstractItemPacket
{
	private L2ItemInstance _item;

	public ExRpItemLink(L2ItemInstance item)
	{
		_item = item;
	}

	@Override
	protected final void writeImpl()
	{
		if(_item == null)
			return;
		//dddhdhhhdddddddddd
		writeC(EXTENDED_PACKET);
		writeH(0x6D);

		writeItemInfo(_item);
	}
}