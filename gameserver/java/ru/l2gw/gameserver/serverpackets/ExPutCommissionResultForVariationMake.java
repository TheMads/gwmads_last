package ru.l2gw.gameserver.serverpackets;

public class ExPutCommissionResultForVariationMake extends L2GameServerPacket
{
	private int _gemstoneObjId;
	private int _itemId;
	private long _gemstoneCount;
	private int _unk1;
	private int _unk2;
	private int _unk3;

	public ExPutCommissionResultForVariationMake(int gemstoneObjId, int itemId, long count)
	{
		_gemstoneObjId = gemstoneObjId;
		_itemId = itemId;
		_gemstoneCount = count;
		_unk1 = 0;
		_unk2 = 0;
		_unk3 = 1;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x56);
		writeD(_gemstoneObjId);
		writeD(_itemId);
		writeQ(_gemstoneCount);
		writeD(_unk1);
		writeD(_unk2);
		writeD(_unk3);
	}
}