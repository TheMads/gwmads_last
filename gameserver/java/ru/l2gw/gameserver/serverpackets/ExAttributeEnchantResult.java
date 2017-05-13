package ru.l2gw.gameserver.serverpackets;

public class ExAttributeEnchantResult extends L2GameServerPacket
{
	private int _result;

	public ExAttributeEnchantResult(int unknown)
	{
		_result = unknown;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x62);
		writeD(_result); //2 - отменено/неудачно
	}
}