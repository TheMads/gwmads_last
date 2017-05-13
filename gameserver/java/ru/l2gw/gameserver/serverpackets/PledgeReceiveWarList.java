package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Clan;

public class PledgeReceiveWarList extends L2GameServerPacket
{
	private FastList<WarInfo> infos = new FastList<WarInfo>();
	private static int _updateType;
	@SuppressWarnings("unused")
	private static int _page;

	public PledgeReceiveWarList(L2Clan clan, int type, int page)
	{
		_updateType = type;
		_page = page;
		infos.clear();
		if(_updateType == 1)
			for(L2Clan _clan : clan.getAttackerClans())
			{
				if(_clan == null)
					continue;
				infos.add(new WarInfo(_clan.getName(), 1, 0));
			}
		else if(_updateType == 0)
			for(L2Clan _clan : clan.getEnemyClans())
			{
				if(_clan == null)
					continue;
				infos.add(new WarInfo(_clan.getName(), _clan.isAtWarWith(clan.getClanId()) && clan.isAtWarWith(_clan.getClanId()) ? 2 : 0, 0));
			}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x40);
		writeD(_updateType); //which type of war list sould be revamped by this packet
		writeD(0x00); //page number goes here(_page ), made it static cuz not sure how many war to add to one page so TODO here
		writeD(infos.size());
		for(WarInfo _info : infos)
		{
			writeS(_info.clan_name);
			writeD(_info.warState); // 0 - Declared, 1 - Under Attack, 2 - Declare War
			writeD(_info.unk2); //filler ??
		}
	}

	static class WarInfo
	{
		public String clan_name;
		public int warState, unk2;

		public WarInfo(String _clan_name, int _warState, int _unk2)
		{
			clan_name = _clan_name;
			warState = _warState;
			unk2 = _unk2;
		}
	}
}