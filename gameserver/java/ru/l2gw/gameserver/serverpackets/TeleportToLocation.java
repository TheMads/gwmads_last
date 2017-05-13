package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Object;

/**
 * format  dddd
 *
 * sample
 * 0000: 3a  69 08 10 48  02 c1 00 00  f7 56 00 00  89 ea ff    :i..H.....V.....
 * 0010: ff  0c b2 d8 61                                     ....a
 */
public class TeleportToLocation extends L2GameServerPacket
{
	private int _targetId;
	private int _x;
	private int _y;
	private int _z;
	private int _heading;

	public TeleportToLocation(L2Object cha, int x, int y, int z)
	{
		_targetId = cha.getObjectId();
		_x = x;
		_y = y;
		_z = z;
		_heading = cha.getHeading();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x22);
		writeD(_targetId);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(0x00); // ?
		writeD(_heading);
		writeD(0x00); // wtf????
	}
}