package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.entity.siege.SiegeClan;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.tables.ClanTable;

/**
 * Populates the Siege Defender List in the SiegeInfo Window<BR>
 * <BR>
 * packet type id 0xcb<BR>
 * format: cddddddd + dSSdddSSd<BR>
 * <BR>
 * c = 0xcb<BR>
 * d = CastleID<BR>
 * d = unknow (0x00)<BR>
 * d = unknow (0x01)<BR>
 * d = unknow (0x00)<BR>
 * d = Number of Defending Clans?<BR>
 * d = Number of Defending Clans<BR>
 * { //repeats<BR>
 * d = ClanID<BR>
 * S = ClanName<BR>
 * S = ClanLeaderName<BR>
 * d = ClanCrestID<BR>
 * d = signed time (seconds)<BR>
 * d = Type -> Owner = 0x01 || Waiting = 0x02 || Accepted = 0x03<BR>
 * d = AllyID<BR>
 * S = AllyName<BR>
 * S = AllyLeaderName<BR>
 * d = AllyCrestID<BR>
 */
public class CastleSiegeDefenderList extends L2GameServerPacket
{
	private SiegeUnit unit;

	public CastleSiegeDefenderList(SiegeUnit unit)
	{
		this.unit = unit;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xCB);
		writeD(unit.getId());
		writeD(0x00); // 0
		writeD(0x01); // 1
		writeD(0x00); // 0
		int size = unit.getSiege().getDefenderClans().size() + unit.getSiege().getDefenderWaitingClans().size();
		if(size > 0)
		{
			L2Clan clan;

			writeD(size);
			writeD(size);
			// Listing the Lord and the approved clans
			for(SiegeClan siegeclan : unit.getSiege().getDefenderClans().values())
			{
				clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
				if(clan == null)
					continue;

				writeD(clan.getClanId());
				writeS(clan.getName());
				writeS(clan.getLeaderName());
				writeD(clan.getCrestId());
				writeD(0x00); // signed time (seconds) (not storated by L2F)
				switch(siegeclan.getType())
				{
					case OWNER:
						writeD(0x01); // owner
						break;
					case DEFENDER_PENDING:
						writeD(0x02); // waiting approved
						break;
					case DEFENDER:
						writeD(0x03); // approved
						break;
					default:
						writeD(0x00);
						break;
				}
				writeD(clan.getAllyId());
				if(clan.getAlliance() != null)
				{
					writeS(clan.getAlliance().getAllyName());
					writeS(clan.getAlliance().getAllyLeaderName()); // AllyLeaderName
					writeD(clan.getAlliance().getAllyCrestId());
				}
				else
				{
					writeS("");
					writeS("");
					writeD(0);
				}
			}
			for(SiegeClan siegeclan : unit.getSiege().getDefenderWaitingClans().values())
			{
				clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
				writeD(clan.getClanId());
				writeS(clan.getName());
				writeS(clan.getLeaderName());
				writeD(clan.getCrestId());
				writeD(0x00); // signed time (seconds) (not storated by L2F)
				writeD(0x02); // waiting approval
				writeD(clan.getAllyId());
				if(clan.getAlliance() != null)
				{
					writeS(clan.getAlliance().getAllyName());
					writeS(clan.getAlliance().getAllyLeaderName()); //AllyLeaderName
					writeD(clan.getAlliance().getAllyCrestId());
				}
				else
				{
					writeS("");
					writeS("");
					writeD(0);
				}
			}
		}
		else
		{
			writeD(0x00);
			writeD(0x00);
		}
	}
}
