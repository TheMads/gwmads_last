package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Clan.RankPrivs;

import java.util.Collection;

public class PledgePowerGradeList extends L2GameServerPacket
{
	private Collection<RankPrivs> _privs;

	public PledgePowerGradeList(Collection<RankPrivs> privs)
	{
		_privs = privs;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x3D);
		writeD(_privs.size());
		for(RankPrivs element : _privs)
		{
			writeD(element.getRank());
			writeD(element.getParty());
		}
	}
}