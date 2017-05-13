package ru.l2gw.gameserver.serverpackets;

public class ExPutItemResultForVariationMake extends L2GameServerPacket
{
	private int _itemObjId;
	private int _itemId;
	private int _unk2;

	public ExPutItemResultForVariationMake(int itemId, int itemObjId)
	{
		_itemObjId = itemObjId;
		_itemId = itemId;
		_unk2 = 1;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x54);
		writeD(_itemObjId);
		writeD(_itemId);
		writeD(_unk2);
	}
}