package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.entity.olympiad.*;

/**
 * @author admin
 * @date 03.02.11 12:17
 */
public class ExReceiveOlympiad extends L2GameServerPacket
{
	private int type;
	private GArray<OlympiadGame> _gameList;
	private OlympiadGame _og;

	public ExReceiveOlympiad()
	{
		type = 0;
		_gameList = new GArray<OlympiadGame>();
		for(OlympiadInstance oi : Olympiad.getOlympiadInstances())
			if(oi != null && oi.getOlympiadGame() != null && (oi.getOlympiadGame().getGameState() == OlympiadGameState.PREPARE || oi.getOlympiadGame().getGameState() == OlympiadGameState.FIGHT))
				_gameList.add(oi.getOlympiadGame());
	}

	public ExReceiveOlympiad(OlympiadGame og)
	{
		type = 1;
		_og = og;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xD5);
		writeD(type); // unknown
		if(type == 0)
		{
			writeD(_gameList.size());
			writeD(0x00); // unknown
			for(OlympiadGame og : _gameList)
			{
				writeD(og.getAreanId());
				writeD(og.getGameType()); // 0 - 3vs3 team, 1 - 1v1 non class, 2 - 1v1 class match, 3
				writeD(og.getGameState().ordinal()); // 1 - standby, 2 - playing
				writeS(og.getTeam(1).getName());
				writeS(og.getTeam(0).getName());
			}
		}
		else
		{
			writeD(_og.getWinnerName().isEmpty() ? 0x01 : 0x00);
			writeS(_og.getWinnerName());
			OlympiadTeam team1 = _og.getTeam(1);
			OlympiadTeam team2 = _og.getTeam(0);
			writeD(1);
			writeD(team1.getPlayersInfo().size());
			for(OlympiadUserInfo oui : team1.getPlayersInfo())
			{
				writeS(oui.getName());
				writeS(oui.getClanName());
				writeD(oui.getClanId());  // clanId ? clanCrestId ?
				writeD(oui.getClassId());
				writeD(oui.getDamage());
				writeD(oui.getPoints());
				writeD(oui.getMatchPoints());
			}
			writeD(2);
			writeD(team2.getPlayersInfo().size());
			for(OlympiadUserInfo oui : team2.getPlayersInfo())
			{
				writeS(oui.getName());
				writeS(oui.getClanName());
				writeD(oui.getClanId());  // clanId ? clanCrestId ?
				writeD(oui.getClassId());
				writeD(oui.getDamage());
				writeD(oui.getPoints());
				writeD(oui.getMatchPoints());
			}
		}
	}
}