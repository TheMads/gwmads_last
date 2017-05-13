package ru.l2gw.gameserver.serverpackets;

/**
 * Format: ch (trigger)
 */
public class ExShowAdventurerGuideBook extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		//System.out.println("ExShowAdventurerGuideBook[24]");
		writeC(EXTENDED_PACKET);
		writeH(0x38);
	}
}
