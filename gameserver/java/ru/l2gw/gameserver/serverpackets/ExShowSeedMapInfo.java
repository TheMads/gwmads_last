package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.instancemanager.FieldCycleManager;
import ru.l2gw.gameserver.instancemanager.ServerVariables;
import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 15.09.2010 17:46:53
 */
public class ExShowSeedMapInfo extends L2GameServerPacket
{
	/*
	2766    1    a,Seed of Infinity Stage 1 Attack In Progress\0
	2767    1    a,Seed of Infinity Stage 2 Attack In Progress\0
	2768    1    a,Seed of Infinity Conquest Complete\0    0
	2769    1    a,Seed of Infinity Stage 1 Defense In Progress\0
	2770    1    a,Seed of Infinity Stage 2 Defense In Progress\0
	2771    1    a,Seed of Destruction Attack in Progress\0
	2772    1    a,Seed of Destruction Conquest Complete\0
	2773    1    a,Seed of Destruction Defense in Progress\0
	 */

	private static final Location[] ENTRANCES =
			{
					new Location(-246857, 251960, 4331), // Seed of Destruction
					new Location(-213770, 210760, 4400), // Seed of Immortality
			};

	private int[] _stages;

	public ExShowSeedMapInfo()
	{
		_stages = new int[] {ServerVariables.getInt("sod_stage", 1)  + 2770, FieldCycleManager.getMapString(3)};
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET); // Id
		writeH(0xA2); // SubId
		writeD(ENTRANCES.length);
		for(int i = 0; i < ENTRANCES.length; i++)
		{
			writeD(ENTRANCES[i].getX());
			writeD(ENTRANCES[i].getY());
			writeD(ENTRANCES[i].getZ());
			writeD(_stages[i]);
		}
	}
}
