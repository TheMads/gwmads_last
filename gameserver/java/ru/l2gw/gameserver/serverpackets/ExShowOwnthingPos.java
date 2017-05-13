package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.instances.L2TerritoryWardInstance;

/**
 * @author rage
 * @date 09.07.2010 14:09:00
 */
public class ExShowOwnthingPos extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x94);

		GArray<L2TerritoryWardInstance> wards = TerritoryWarManager.getWar().getWards();
		writeD(wards.size());
		for(L2TerritoryWardInstance ward : wards)
		{
			writeD(ward.getTerritoryId());

			if(!ward.isDead())
			{
				writeD(ward.getX());
				writeD(ward.getY());
				writeD(ward.getZ());
			}
			else if(ward.getPlayer() != null)
			{
				writeD(ward.getPlayer().getX());
				writeD(ward.getPlayer().getY());
				writeD(ward.getPlayer().getZ());
			}
			else
			{
				writeD(0);
				writeD(0);
				writeD(0);
			}
		}
	}
}
