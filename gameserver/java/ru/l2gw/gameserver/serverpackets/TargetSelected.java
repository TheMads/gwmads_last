package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.util.Location;

/**
 * format   dddddd
 *
 * sample
 * 0000: 23  0b 07 10 48  3e 31 10 48  3a f6 00 00  91 5b 00
 * 0010: 00  4c f1 ff ff  00 00 00 00
 */
public class TargetSelected extends L2GameServerPacket
{
	private int _objectId;
	private int _targetId;
	private Location _loc;

	public TargetSelected(int objectId, int targetId, Location loc)
	{
		_objectId = objectId;
		_targetId = targetId;
		_loc = loc;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x23);
		writeD(_objectId);
		writeD(_targetId);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(0x00);
	}
}