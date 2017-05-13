package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 16:39
 */
public class ExCubeGameRequestReady extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x97);
		writeD(0x04);
	}
}
