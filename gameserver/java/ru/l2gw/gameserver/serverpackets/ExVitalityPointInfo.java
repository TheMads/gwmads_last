package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 13.08.2010 16:00:10
 */
public class ExVitalityPointInfo extends L2GameServerPacket
{
	private final int _points;

	public ExVitalityPointInfo(int points)
	{
		_points = points;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xA1);
		writeD(_points);
	}
}
