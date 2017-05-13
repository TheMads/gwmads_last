package ru.l2gw.gameserver.serverpackets;

/**
 * Format: ch
 */
public class ExPlayScene extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x5D);
		writeD(0x00); //Kamael
	}
}