package ru.l2gw.gameserver.serverpackets;

public class ExDuelEnd extends L2GameServerPacket
{
	private int _duelType;

	public ExDuelEnd(int duelType)
	{
		_duelType = duelType;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x50);
		writeD(_duelType); //хз почему 0, возможно 1 для party duel. нет возможности проверить.
	}
}