package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Clan;

public class PledgeShowInfoUpdate extends L2GameServerPacket
{
	private int clan_id, clan_level, clan_rank, clan_rep, clan_crest, at_war;
	private int ally_id = 0;
	private int ally_crestid = 0;
	private String ally_name = "";
	private int hasCastle, hasHideout, hasFortress, territoryId;

	public PledgeShowInfoUpdate(final L2Clan clan)
	{
		clan_id = clan.getClanId();
		clan_level = clan.getLevel();
		clan_crest = clan.getCrestId();
		ally_id = clan.getAllyId();
		if(clan.getAlliance() != null)
		{
			ally_name = clan.getAlliance().getAllyName();
			ally_id = clan.getAlliance().getAllyId();
			ally_crestid = clan.getAlliance().getAllyCrestId();
		}
		at_war = clan.isAtWar();
		hasCastle = clan.getHasCastle();
		hasHideout = clan.getHasHideout();
		hasFortress = clan.getHasFortress();
		territoryId = clan.getTerritoryId();
		clan_rank = clan.getRank();
		clan_rep = clan.getReputationScore();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x8E);
		//sending empty data so client will ask all the info in response ;)
		writeD(clan_id);
		writeD(clan_crest);
		writeD(clan_level);
		writeD(hasCastle);
		writeD(0); // ??? 0
		writeD(hasHideout);
		writeD(hasFortress);
		writeD(clan_rank);// displayed in the "tree" view (with the clan skills)
		writeD(clan_rep);
		writeD(0);
		writeD(0);

		writeD(ally_id); //c5
		writeS(ally_name); //c5
		writeD(ally_crestid); //c5
		writeD(at_war); //c5

		writeD(0x00); // Unknown
		writeD(territoryId); // Registered Territori ID
	}
}