package ru.l2gw.gameserver.serverpackets;

public class ExVariationResult extends L2GameServerPacket
{
	private int _stat12;
	private int _stat34;
	private int _unk3;

	public ExVariationResult(int unk1, int unk2, int unk3)
	{
		_stat12 = unk1;
		_stat34 = unk2;
		_unk3 = unk3;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x57);
		writeD(_stat12);
		writeD(_stat34);
		writeD(_unk3);
	}
}