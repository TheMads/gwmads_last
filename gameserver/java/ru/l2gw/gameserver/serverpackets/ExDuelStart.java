package ru.l2gw.gameserver.serverpackets;

public class ExDuelStart extends L2GameServerPacket
{
	private int _duelType;

	public ExDuelStart(int duelType)
	{
		_duelType = duelType;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x4F);
		writeD(_duelType); // неизвестный, возможно тип дуэли.
	}
}