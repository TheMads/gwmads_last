package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;

public class ExShowCastleInfo extends L2GameServerPacket
{
	private FastList<CastleInfo> infos = new FastList<CastleInfo>();

	public ExShowCastleInfo()
	{
		String owner_name;
		int _id, tax, next_siege;

		for(Castle castle : ResidenceManager.getInstance().getCastleList())
		{
			owner_name = castle.getOwner() == null ? "" : castle.getOwner().getName();
			_id = castle.getId();
			tax = castle.getTaxPercent();
			next_siege = (int) (castle.getSiege().getSiegeDate().getTimeInMillis() / 1000) ;
			infos.add(new CastleInfo(owner_name, _id, tax, next_siege));
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x14);
		writeD(infos.size());
		for(CastleInfo _info : infos)
		{
			writeD(_info._id);
			writeS(_info.owner_name);
			writeD(_info.tax);
			writeD(_info.next_siege);
		}
		infos.clear();
	}

	static class CastleInfo
	{
		public String owner_name;
		public int _id, tax, next_siege;

		public CastleInfo(String _owner_name, int __id, int _tax, int _next_siege)
		{
			owner_name = _owner_name;
			_id = __id;
			tax = _tax;
			next_siege = _next_siege;
		}
	}
}