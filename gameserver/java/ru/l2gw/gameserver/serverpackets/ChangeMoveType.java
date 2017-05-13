package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;

/**
 *
 * sample
 *
 * 0000: 3e 2a 89 00 4c 01 00 00 00                         .|...
 *
 * format   dd
 */
public class ChangeMoveType extends L2GameServerPacket
{
	private int _chaId;
	private boolean _running;

	public ChangeMoveType(L2Character cha)
	{
		_chaId = cha.getObjectId();
		_running = cha.isRunning();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x28);
		writeD(_chaId);
		writeD(_running ? 1 : 0);
		writeD(0); //c2
	}
}
