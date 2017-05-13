package ru.l2gw.gameserver.serverpackets;

/**
 * @author: rage
 * @date: 27.09.11 0:16
 */
public class ExChangeZoneInfo extends L2GameServerPacket
{
	private final int zoneId, zoneStatus;

	public ExChangeZoneInfo(int zone, int status)
	{
		zoneId = zone;
		zoneStatus = status;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xC2);
		writeD(zoneId);
		writeD(zoneStatus);
	}
}