package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Alliance;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.tables.ClanTable;

public class AllianceInfo extends L2GameServerPacket
{
	private final L2Player _player;

	public AllianceInfo(L2Player player)
	{
		_player = player;
	}

	@Override
	final public void runImpl()
	{
		SystemMessage sm;
		if(_player.getAllyId() <= 0)
			return;

		L2Alliance ally = _player.getAlliance();
		_player.sendPacket(new SystemMessage(SystemMessage._ALLIANCE_INFORMATION_));
		_player.sendPacket(new SystemMessage(SystemMessage.ALLIANCE_NAME_S1).addString(_player.getClan().getAlliance().getAllyName()));
		int clancount = 0;
		L2Clan leaderclan = ally.getLeader();
		clancount = ClanTable.getInstance().getAlliance(leaderclan.getAllyId()).getMembers().length;
		int[] online = new int[clancount + 1];
		int[] count = new int[clancount + 1];
		L2Clan[] clans = ally.getMembers();
		for(int i = 0; i < clancount; i++)
		{
			online[i + 1] = clans[i].getOnlineMembers(null).size();
			count[i + 1] = clans[i].getMembers().length;
			online[0] += online[i + 1];
			count[0] += count[i + 1];
		}
		//Connection
		sm = new SystemMessage(SystemMessage.CONNECTION_S1_TOTAL_S2);
		sm.addNumber(online[0]);
		sm.addNumber(count[0]);
		_player.sendPacket(sm);
		sm = new SystemMessage(SystemMessage.ALLIANCE_LEADER_S2_OF_S1);
		sm.addString(leaderclan.getName());
		sm.addString(leaderclan.getLeaderName());
		_player.sendPacket(sm);
		//clan count
		_player.sendPacket(new SystemMessage(SystemMessage.AFFILIATED_CLANS_TOTAL_S1_CLAN_S).addNumber(clancount));
		_player.sendPacket(new SystemMessage(SystemMessage._CLAN_INFORMATION_));
		for(int i = 0; i < clancount; i++)
		{
			_player.sendPacket(new SystemMessage(SystemMessage.CLAN_NAME_S1).addString(clans[i].getName()));
			_player.sendPacket(new SystemMessage(SystemMessage.CLAN_LEADER_S1).addString(clans[i].getLeaderName()));
			_player.sendPacket(new SystemMessage(SystemMessage.CLAN_LEVEL_S1).addNumber(clans[i].getLevel()));
			sm = new SystemMessage(SystemMessage.CONNECTION_S1_TOTAL_S2);
			sm.addNumber(online[i + 1]);
			sm.addNumber(count[i + 1]);
			_player.sendPacket(sm);
			_player.sendPacket(new SystemMessage(SystemMessage.__DASHES__));
		}
		_player.sendPacket(new SystemMessage(SystemMessage.__EQUALS__));
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xB5);
	}
}