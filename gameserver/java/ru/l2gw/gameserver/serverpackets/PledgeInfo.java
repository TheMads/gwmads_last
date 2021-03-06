package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Clan;

public class PledgeInfo extends L2GameServerPacket
{
	private int clan_id;
	private String clan_name, ally_name;

	public PledgeInfo(L2Clan clan)
	{
		clan_id = clan.getClanId();
		clan_name = clan.getName();
		ally_name = clan.getAlliance() == null ? "" : clan.getAlliance().getAllyName();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x89);
		writeD(clan_id);
		writeS(clan_name);
		writeS(ally_name);
	}
}