package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.tables.ClanTable;

public class ExShowFortressInfo extends L2GameServerPacket
{
	private FastList<FortressInfo> infos = new FastList<FortressInfo>();

	public ExShowFortressInfo()
	{
		int fort_id, fort_status, fort_siege;
		String fort_owner;

		for(Fortress fortress : ResidenceManager.getInstance().getFortressList())
		{
			fort_id = fortress.getId();
			L2Clan clan = null;
			if(fortress.getOwnerId() > 0 && (clan = ClanTable.getInstance().getClan(fortress.getOwnerId())) == null)
				fortress.setOwnerId(0);
			fort_owner = clan == null ? "" : clan.getName();
			fort_status = fortress.getSiege().isInProgress() ? 1 : 0; //status? 0 - At Peace, 1 - In War
			fort_siege = fortress.getOwnerId() == 0 ? 0 : (int)(System.currentTimeMillis() / 1000 - fortress.getLastSiegeDate());
			infos.add(new FortressInfo(fort_owner, fort_id, fort_status, fort_siege)); //time held в секундах
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x15);
		writeD(infos.size());
		for(FortressInfo _info : infos)
		{
			writeD(_info.fort_id);
			writeS(_info.fort_owner);
			writeD(_info.fort_status);
			writeD(_info.fort_siege);
		}
		infos.clear();
	}

	static class FortressInfo
	{
		public int fort_id, fort_status, fort_siege;
		public String fort_owner;

		public FortressInfo(String _fort_owner, int _fort_id, int _fort_status, int _fort_siege)
		{
			fort_owner = _fort_owner;
			fort_id = _fort_id;
			fort_status = _fort_status;
			fort_siege = _fort_siege;
		}
	}
}