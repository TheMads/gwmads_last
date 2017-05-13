package ru.l2gw.gameserver.serverpackets;

/**
 * Fromat: (ch)
 * (just a trigger)
 */
public class ExMailArrived extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x2e);
	}
}