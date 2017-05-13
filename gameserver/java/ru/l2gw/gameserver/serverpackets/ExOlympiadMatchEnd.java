package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 */
public class ExOlympiadMatchEnd extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x2D);
	}
}
