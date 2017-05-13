package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Summon;

public class ExPartyPetWindowDelete extends L2GameServerPacket
{
	private final int _summonObjectId;
	private final int _ownerObjectId;
	private final String _summonName;

	public ExPartyPetWindowDelete(L2Summon summon)
	{
		_summonObjectId = summon.getObjectId();
		_summonName = summon.getName();
		_ownerObjectId = summon.getPlayer().getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x6B);
		writeD(_summonObjectId);
		writeD(_ownerObjectId);
		writeS(_summonName);
	}
}