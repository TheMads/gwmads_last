package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;

/**
 * @author rage
 * @date 06.07.2010 18:35:40
 */
public class ExReplyRegisterDominion extends L2GameServerPacket
{
	private final int _territoryId, _regType, _regRequest, _clans, _players;

	public ExReplyRegisterDominion(int territoryId, int regType, int regRequest)
	{
		_territoryId = territoryId;
		_regType = regType;
		_regRequest = regRequest;
		_clans = TerritoryWarManager.getRegisteredClans(_territoryId).size();
		_players = TerritoryWarManager.getRegisteredMerc(_territoryId).size();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x92);
		writeD(_territoryId);
		writeD(_regType);
		writeD(_regRequest);
		writeD(0x01); // unknown
		writeD(_clans);
		writeD(_players);
	}
}

