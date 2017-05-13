package ru.l2gw.gameserver.serverpackets;

public class ExPutEnchantTargetItemResult extends L2GameServerPacket
{
	private int result;

	public ExPutEnchantTargetItemResult(int result)
	{
		this.result = result;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x82);
		writeD(result);
	}
}