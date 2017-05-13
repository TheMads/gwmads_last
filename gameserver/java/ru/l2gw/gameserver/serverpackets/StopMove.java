package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.util.Location;

/**
 * format   ddddd
 *
 * sample
 * 0000: 59 1a 95 20 48 44 17 02 00 03 f0 fc ff 98 f1 ff    Y.. HD..........
 * 0010: ff c1 1a 00 00                                     .....
 */
public class StopMove extends L2GameServerPacket
{
	private final int _objectId;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _heading;

	public StopMove(int objectId, Location loc)
	{
		_objectId = objectId;
		_x = loc.getX();
		_y = loc.getY();
		_z = loc.getZ();
		_heading = loc.getHeading();
	}

	public StopMove(L2Character cha)
	{
		_objectId = cha.getObjectId();
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_heading = cha.getHeading();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x47);
		writeD(_objectId);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
	}
}