package ru.l2gw.gameserver.serverpackets;

import gnu.trove.map.hash.TIntIntHashMap;
import ru.l2gw.gameserver.instancemanager.RaidBossSpawnManager;

/**
 * Format: ch ddd [ddd]
 */
public class ExGetBossRecord extends L2GameServerPacket
{
	private RaidBossSpawnManager.PlayerRaidPoints _prp;

	public ExGetBossRecord(RaidBossSpawnManager.PlayerRaidPoints prp)
	{
		_prp = prp;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x34);

		if(_prp != null)
		{
			writeD(_prp.getRank()); // char ranking
			writeD(_prp.getTotalPoints()); // char total points

			TIntIntHashMap pp = _prp.getPoints();
			writeD(pp.size()); // list size
			for(int bossId : pp.keys())
			{
				writeD(bossId);
				writeD(pp.get(bossId));
				writeD(0);// don`t know
			}
		}
		else
		{
			writeD(0x00); // char ranking
			writeD(0x00); // char total points
			writeD(0x00); // list size
		}
	}
}
