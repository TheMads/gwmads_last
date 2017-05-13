package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.util.Location;

/**
 * format  ddddd
 *
 * sample
 * 0000: 24  69 08 10 48  02 c1 00 00  f7 56 00 00  89 ea ff
 * 0010: ff  0c b2 d8 61  01 00 00 00
 */
public class TargetUnselected extends L2GameServerPacket
{
	private int _targetId, _manual;
	private Location _loc;

	/**
	 * @param character
	 */
	public TargetUnselected(L2Character cha, boolean manual)
	{
		_targetId = cha.getObjectId();
		_loc = cha.getLoc();
		_manual = manual ? 1 : 0;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x24);
		writeD(_targetId);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(_manual); // иногда бывает 1
	}
}