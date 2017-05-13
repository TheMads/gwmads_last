package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;

public class FinishRotating extends L2GameServerPacket
{
	private int _charId, _degree, _speed;

	public FinishRotating(L2Character cha, int degree, int speed)
	{
		_charId = cha.getObjectId();
		_degree = degree;
		_speed = speed;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x61);
		writeD(_charId);
		writeD(_degree);
		writeD(_speed);
		writeD(0x00); // Unknown
	}
}