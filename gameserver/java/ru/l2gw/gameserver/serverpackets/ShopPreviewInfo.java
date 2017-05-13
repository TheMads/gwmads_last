package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 19.10.2010 1:36:49
 */
public class ShopPreviewInfo extends L2GameServerPacket
{
	private final int[] _items;

	public ShopPreviewInfo(int[] previewItems)
	{
		_items = previewItems;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xF6);
		writeD(_items.length);
		for(int itemId : _items)
			writeD(itemId);
	}
}
