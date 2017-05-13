package ru.l2gw.gameserver.serverpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.L2Character;

/**
 * 0000: 75  7a 07 80 49  63 27 00 4a  ea 01 00 00  c1 37 fe    uz..Ic'.J.....7. <p>
 * 0010: ff 9e c3 03 00 8f f3 ff ff                         .........<p>
 * <p>
 *
 * format   dddddd		(player id, target id, distance, startx, starty, startz)<p>
 */
public class MoveToPawn extends L2GameServerPacket
{
	private static Log _log = LogFactory.getLog(MoveToPawn.class.getName());

	private int _chaId;
	private int _targetId;
	private int _distance;
	private int _x, _y, _z, _tx, _ty, _tz;

	public MoveToPawn(L2Character cha, L2Character target, int distance)
	{
		if(cha == target)
		{
			_log.warn("Try pawn to yourself!");
			Thread.dumpStack();
			_chaId = 0;
			return;
		}

		if(target == null)
		{
			_chaId = 0;
			return;
		}

		_chaId = cha.getObjectId();
		_targetId = target.getObjectId();
		_distance = distance;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_tx = target.getX();
		_ty = target.getY();
		_tz = target.getZ();
	}

	@Override
	protected final void writeImpl()
	{
		if(_chaId == 0)
			return;

		writeC(0x72);

		writeD(_chaId);
		writeD(_targetId);
		writeD(_distance);

		writeD(_x);
		writeD(_y);
		writeD(_z);

		writeD(_tx);
		writeD(_ty);
		writeD(_tz);
	}
}