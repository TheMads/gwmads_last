package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.util.Location;

/**
 * 0000: 01  7a 73 10 4c  b2 0b 00 00  a3 fc 00 00  e8 f1 ff    .zs.L...........
 * 0010: ff  bd 0b 00 00  b3 fc 00 00  e8 f1 ff ff             .............
 *
 * ddddddd
 */
public class CharMoveToLocation extends L2GameServerPacket
{
	private int _objectId;
	private Location _current;
	private Location _destination;

	public CharMoveToLocation(L2Character cha, Location destination)
	{
		_objectId = cha.getObjectId();
		_current = cha.getLoc();
		_destination = destination;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x2f);

		writeD(_objectId);

		writeD(_destination.getX());
		writeD(_destination.getY());
		writeD(_destination.getZ() + 10);

		writeD(_current.getX());
		writeD(_current.getY());
		writeD(_current.getZ());
	}
}