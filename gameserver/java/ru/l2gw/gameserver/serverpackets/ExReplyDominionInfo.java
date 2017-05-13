package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.entity.Territory;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 06.07.2010 12:01:35
 */
public class ExReplyDominionInfo extends L2GameServerPacket
{
	private GArray<Territory> _territories;

	public ExReplyDominionInfo()
	{
		_territories = TerritoryWarManager.getTerritories();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x93);
		writeD(_territories.size());
		for(Territory terr : _territories)
		{
			writeD(terr.getId());
			writeS(terr.getName());
			writeS(terr.getOwnerClanName());
			writeD(terr.getWards().size());
			for(int wardId : terr.getWards())
				writeD(wardId);
			writeD((int) (TerritoryWarManager.getWarDate().getTimeInMillis() / 1000));
		}
	}
}
