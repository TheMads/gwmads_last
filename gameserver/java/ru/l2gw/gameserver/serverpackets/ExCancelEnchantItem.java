package ru.l2gw.gameserver.serverpackets;

public class ExCancelEnchantItem extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x82);
		writeD(57);
	}
}