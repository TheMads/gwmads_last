package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Summon;

public class PetStatusShow extends L2GameServerPacket
{
	private int _summonType;

	public PetStatusShow(L2Summon summon)
	{
		_summonType = summon.getSummonType();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xB1);
		writeD(_summonType);
	}
}