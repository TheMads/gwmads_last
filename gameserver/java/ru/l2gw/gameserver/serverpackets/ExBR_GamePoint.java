package ru.l2gw.gameserver.serverpackets;

/**
 * @author: rage
 * @date: 15.10.11 21:39
 */
public class ExBR_GamePoint extends L2GameServerPacket
{
	private final int objectId;
	private final long points;

	public ExBR_GamePoint(int objectId, long points)
	{
		this.objectId = objectId;
		this.points = points;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xD6);
		writeD(objectId);
		writeQ(points);
		writeD(0x00);
	}
}
