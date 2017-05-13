package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Summon;

public class SetSummonRemainTime extends L2GameServerPacket
{
	private final int _maxFed;
	private final int _curFed;

	/**
	 * packet type id 0xd1
	 * format: cdd
	 * @param summon
	 */
	public SetSummonRemainTime(L2Summon summon)
	{
		_curFed = summon.getCurrentFed();
		_maxFed = summon.getMaxMeal();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xD1);
		writeD(_maxFed);
		writeD(_curFed);
	}
}