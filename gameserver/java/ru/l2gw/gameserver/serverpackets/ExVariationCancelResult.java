package ru.l2gw.gameserver.serverpackets;

public class ExVariationCancelResult extends L2GameServerPacket
{
	private int _succsess;

	public ExVariationCancelResult(boolean success)
	{
		_succsess = success ? 1 : 0;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x59);
		writeD(_succsess);
		writeD(0x01);
	}
}