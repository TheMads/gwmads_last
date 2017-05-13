package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Territory;

/**
 * @author rage
 * @date 06.07.2010 15:52:17
 */
public class ExShowDominionRegistry extends L2GameServerPacket
{
	private Territory _territory;
	private int _regClans, _regMercs, _warTime, _clanRegTerritoryId, _mercRegTettitoryId;

	public ExShowDominionRegistry(L2Player player, Territory territory)
	{
		_territory = territory;
		_regClans = TerritoryWarManager.getRegisteredClans(_territory.getId()).size();
		_regMercs = TerritoryWarManager.getRegisteredMerc(_territory.getId()).size();
		_clanRegTerritoryId = player.getClanId() > 0 ? TerritoryWarManager.getClanRegisteredTerritoryId(player.getClanId()) : 0;
		_mercRegTettitoryId = TerritoryWarManager.getMercRegisteredTerritoryId(player.getObjectId());
		_warTime = (int) (TerritoryWarManager.getWarDate().getTimeInMillis() / 1000);
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x91);
		writeD(_territory.getId());
		L2Clan owner = _territory.getOwner();
		if(owner != null)
		{
			writeS(owner.getName());
			writeS(owner.getLeaderName());
			writeS(owner.getAlliance() != null ? owner.getAlliance().getAllyName() : "");
		}
		else
		{
			writeS("");
			writeS("");
			writeS("");
		}
		writeD(_regClans);
		writeD(_regMercs);
		writeD(_warTime);
		writeD((int) (System.currentTimeMillis() / 1000));
		writeD(_clanRegTerritoryId);
		writeD(_mercRegTettitoryId);
		writeD(0x01);
		GArray<Territory> territories = TerritoryWarManager.getTerritories();
		writeD(territories.size());
		for(Territory terr : territories)
		{
			writeD(terr.getId());
			writeD(terr.getWards().size());
			for(int wardId : terr.getWards())
				writeD(wardId);
		}
	}
}
