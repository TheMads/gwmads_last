package ru.l2gw.gameserver.serverpackets;

public class ExPutIntensiveResultForVariationMake extends L2GameServerPacket
{
	private int _refinerItemObjId;
	private int _lifestoneItemId;
	private int _gemstoneItemId;
	private long _gemstoneCount;
	private int _unk2;

	public ExPutIntensiveResultForVariationMake(int refinerItemObjId, int lifeStoneId, int gemstoneItemId, long gemstoneCount)
	{
		_refinerItemObjId = refinerItemObjId;
		_lifestoneItemId = lifeStoneId;
		_gemstoneItemId = gemstoneItemId;
		_gemstoneCount = gemstoneCount;
		_unk2 = 1;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x55);
		writeD(_refinerItemObjId);
		writeD(_lifestoneItemId);
		writeD(_gemstoneItemId);
		writeQ(_gemstoneCount);
		writeD(_unk2);
	}
}