package ru.l2gw.gameserver.model.entity.siege;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.tables.ClanTable;

public class SiegeClan
{
	private int _clanId = 0;
	private SiegeClanType _type;

	public SiegeClan(int clanId, SiegeClanType type)
	{
		_clanId = clanId;
		_type = type;
	}

	public int getClanId()
	{
		return _clanId;
	}

	public L2Clan getClan()
	{
		return ClanTable.getInstance().getClan(_clanId);
	}

	public SiegeClanType getType()
	{
		return _type;
	}

	public void setTypeId(SiegeClanType type)
	{
		_type = type;
	}
}