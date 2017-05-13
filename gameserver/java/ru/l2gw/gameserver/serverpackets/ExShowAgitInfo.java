package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.tables.ClanTable;

public class ExShowAgitInfo extends L2GameServerPacket
{
	private FastList<AgitInfo> infos = new FastList<AgitInfo>();

	public ExShowAgitInfo()
	{
		String clan_name, leader_name;
		int ch_id, acquired;

		for(ClanHall clanHall : ResidenceManager.getInstance().getClanHallList())
		{
			ch_id = clanHall.getId();
			acquired = clanHall.haveRolePlaySiege() ? 2 : clanHall.getSiegeZone() != null ? 1 : 0;

			L2Clan clan = ClanTable.getInstance().getClan(clanHall.getOwnerId());
			clan_name = clanHall.getOwnerId() == 0 || clan == null ? "" : clan.getName();
			leader_name = clanHall.getOwnerId() == 0 || clan == null ? "" : clan.getLeader().getName();
			infos.add(new AgitInfo(clan_name, leader_name, ch_id, acquired));
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x16);
		writeD(infos.size());
		for(AgitInfo _info : infos)
		{
			writeD(_info.ch_id);
			writeS(_info.clan_name);
			writeS(_info.leader_name);
			writeD(_info.lease);
		}
	}

	static class AgitInfo
	{
		public String clan_name, leader_name;
		public int ch_id, lease;

		public AgitInfo(String _clan_name, String _leader_name, int _ch_id, int _lease)
		{
			clan_name = _clan_name;
			leader_name = _leader_name;
			ch_id = _ch_id;
			lease = _lease;
		}
	}
}