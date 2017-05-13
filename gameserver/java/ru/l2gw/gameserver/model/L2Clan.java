package ru.l2gw.gameserver.model;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.CrestCache;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.entity.Hero;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.tables.SkillTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class L2Clan implements Comparable<L2Clan>
{
	private static final org.apache.commons.logging.Log _log = LogFactory.getLog(L2Clan.class.getName());
	private static final org.apache.commons.logging.Log logClanRep = LogFactory.getLog("clan");

	private String _name;
	private int _clanId;
	private L2ClanMember _leader = null;
	Map<Integer, L2ClanMember> _members = new FastMap<Integer, L2ClanMember>();
	private String _notice = null;

	private int _allyId;
	private byte _level;
	private int _hasCastle = 0;
	private int _hasFortress = 0;
	private int _hiredGuards;
	private int _hasHideout = 0;
	private int _territoryId = 0;
	private int _crestId;
	private int _crestLargeId;

	private long _expelledMemberTime;
	private long _leavedAllyTime;
	private long _dissolvedAllyTime;

	// siege clan statistic
	private int _siegeKills;
	private int _siegeDeath;

	private L2NpcInstance _camp;

	// all these in milliseconds
	public static long EXPELLED_MEMBER_PENALTY = 24 * 60 * 60 * 1000;
	public static long LEAVED_ALLY_PENALTY = 24 * 60 * 60 * 1000;
	public static long DISSOLVED_ALLY_PENALTY = Config.DaysBeforeCreateNewAllyWhenDissolved * 24 * 60 * 60 * 1000;

	private ClanWarehouse _warehouse = new ClanWarehouse(this);
	private List<L2Clan> _atWarWith = new FastList<L2Clan>();
	private List<L2Clan> _underAttackFrom = new FastList<L2Clan>();

	protected FastMap<Integer, L2Skill> _skills = new FastMap<Integer, L2Skill>();
	protected FastMap<Integer, GArray<L2Skill>> _subPledgeSkills = new FastMap<Integer, GArray<L2Skill>>();
	protected FastMap<Integer, RankPrivs> _privs = new FastMap<Integer, RankPrivs>();
	protected FastMap<Integer, SubPledge> _subPledges = new FastMap<Integer, SubPledge>();

	private int _reputation = 0;
	private int _airshipEp = -1;
	private L2ClanAirship _airship;

	// System Privileges
	public static final int CP_NOTHING = 0;
	public static final int CP_CL_JOIN_CLAN = 2; // Invite
	public static final int CP_CL_GIVE_TITLE = 4; // Manage Title
	public static final int CP_CL_VIEW_WAREHOUSE = 8; // Warehouse search
	public static final int CP_CL_MANAGE_RANKS = 16; // Manage Ranks
	public static final int CP_CL_PLEDGE_WAR = 32; // Clan War
	public static final int CP_CL_DISMISS = 64; // Dismiss
	public static final int CP_CL_REGISTER_CREST = 128; // Edit crest
	public static final int CP_CL_MASTER_RIGHTS = 256; // Apprentice
	public static final int CP_CL_TROOPS_FAME = 512; // Troops/Fame ?
	public static final int CP_CL_SUMMON_AIRSHIP = 1024; // Summon airship
	// Clan Hall
	public static final int CP_CH_OPEN_DOOR = 2048; // Clan hall: Enter/Exit
	public static final int CP_CH_USE_FUNCTIONS = 4096; //Clan hall: Use Functions 
	public static final int CP_CH_AUCTION = 8192; // Clan Hall: Auction
	public static final int CP_CH_DISMISS = 16384; // Clan Hall: Right to Dismiss
	public static final int CP_CH_SET_FUNCTIONS = 32768; // Clan Hall: Set Functions
	// Castle
	public static final int CP_CS_OPEN_DOOR = 65536; // Castle: Enter/Exit
	public static final int CP_CS_MANOR_ADMIN = 131072; // Castle: Manor
	public static final int CP_CS_MANAGE_SIEGE = 262144; // Castle: Siege War
	public static final int CP_CS_USE_FUNCTIONS = 524288; // Castle: Use Functions
	public static final int CP_CS_DISMISS = 1048576; // Castle: Right to Dismiss
	public static final int CP_CS_TAXES = 2097152; // Castle: Manage Taxes
	public static final int CP_CS_MERCENARIES = 4194304; // Castle: Mercenaries
	public static final int CP_CS_SET_FUNCTIONS = 8388608; // Castle: Set functions
	public static final int CP_ALL = 16777214;

	// Sub-unit types
	public static final int SUBUNIT_ACADEMY = -1;
	public static final int SUBUNIT_NONE = 0;
	public static final int SUBUNIT_ROYAL1 = 100;
	public static final int SUBUNIT_ROYAL2 = 200;
	public static final int SUBUNIT_KNIGHT1 = 1001;
	public static final int SUBUNIT_KNIGHT2 = 1002;
	public static final int SUBUNIT_KNIGHT3 = 2001;
	public static final int SUBUNIT_KNIGHT4 = 2002;

	public static enum PledgeRank
	{
		VAGABOND,
		VASSAL,
		HEIR,
		KNIGHT,
		WISEMAN,
		BARON,
		VISCOUNT,
		COUNT,
		MARQUIS,
		DUKE,
		GRAND_DUKE,
		DISTINGUISHED_KING,
		EMPEROR
	}

	private final static ClanReputationComparator REPUTATION_COMPARATOR = new ClanReputationComparator();
	/**
	 * Количество мест в таблице рангов кланов
	 */
	private final static int REPUTATION_PLACES = 100;

	/**
	 * Конструктор используется только внутри для восстановления из базы
	 */
	private L2Clan(int clanId)
	{
		_clanId = clanId;
		InitializePrivs();
	}

	public L2Clan(int clanId, String clanName, L2ClanMember leader)
	{
		_clanId = clanId;
		_name = clanName;
		InitializePrivs();
		setLeader(leader);
	}

	public int getClanId()
	{
		return _clanId;
	}

	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}

	public int getLeaderId()
	{
		return _leader != null ? _leader.getObjectId() : 0;
	}

	public L2ClanMember getLeader()
	{
		return _leader;
	}

	public void setLeader(L2ClanMember leader)
	{
		_leader = leader;
		_members.put(leader.getObjectId(), leader);
	}

	public String getLeaderName()
	{
		return _leader.getName();
	}

	public String getName()
	{
		return _name;
	}

	public void setName(String name)
	{
		_name = name;
	}

	private void addClanMember(L2ClanMember member)
	{
		_members.put(member.getObjectId(), member);
	}

	public void addClanMember(L2Player player)
	{
		L2ClanMember member = new L2ClanMember(this, player.getName(), player.getTitle(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getApprentice(), false, player.getSex(), player.getUserVars());
		addClanMember(member);
		if(player.getPowerGrade() > 0)
			getRankPrivs(player.getPowerGrade()).setParty(countMembersByRank(player.getPowerGrade()));
		Hero.checkHeroForClanAdd(player.getObjectId(), this);
	}

	public L2ClanMember getClanMember(Integer id)
	{
		return _members.get(id);
	}

	public L2ClanMember getClanMember(String name)
	{
		for(L2ClanMember member : _members.values())
			if(member.getName().equalsIgnoreCase(name))
				return member;
		return null;
	}

	public int getMembersCount()
	{
		return _members.size();
	}

	public void flush()
	{
		for(L2ClanMember member : getMembers())
			removeClanMember(member.getObjectId());
		for(L2ItemInstance item : _warehouse.getItemsList())
			_warehouse.destroyItem("FlushClan", item, null, null);
		if(_hasCastle != 0)
			ResidenceManager.getInstance().getBuildingById(_hasCastle).changeOwner(0);
		if(_hasFortress != 0)
			ResidenceManager.getInstance().getBuildingById(_hasFortress).changeOwner(0);
	}

	public void updateMemberId(int oldId,int newId)
	{
		L2ClanMember exMember = _members.remove(oldId);
		exMember.setObjectId(newId);
		_members.put(newId, exMember);
		SubPledge sp = _subPledges.get(exMember.getPledgeType());
		if(sp != null && sp.getLeaderId() == exMember.getObjectId()) // subpledge leader
			sp.setLeaderId(newId); // clan leader has to assign another one, via villagemaster
	}

	public void removeClanMember(int id)
	{
		if(id == getLeaderId())
			return;
		L2ClanMember exMember = _members.remove(id);
		if(exMember == null)
			return;
		SubPledge sp = _subPledges.get(exMember.getPledgeType());
		if(sp != null && sp.getLeaderId() == exMember.getObjectId()) // subpledge leader
			sp.setLeaderId(0); // clan leader has to assign another one, via villagemaster

		Hero.checkHeroForClanRemove(id);

		L2Player player = exMember.getPlayer();
		if(player != null)
		{
			player.setSiegeId(0);
			player.setSiegeState(0);
			removeClanSkills(player);
		}
		removeMemberInDatabase(exMember);
	}

	public void removeClanMember(String name)
	{
		if(name.equals(getLeaderName()))
			return;
		L2ClanMember exMember = getClanMember(name);
		if(exMember == null)
			return;

		Hero.checkHeroForClanRemove(exMember.getObjectId());

		_members.remove(exMember.getObjectId());
		SubPledge sp = _subPledges.get(exMember.getPledgeType());
		if(sp != null && sp.getLeaderId() == exMember.getObjectId()) // subpledge leader
			sp.setLeaderId(0); // clan leader has to assign another one, via villagemaster

		L2Player player = exMember.getPlayer();
		if(player != null)
		{
			player.setSiegeId(0);
			player.setSiegeState(0);
			removeClanSkills(player);
		}
		removeMemberInDatabase(exMember);
	}

	public L2ClanMember[] getMembers()
	{
		return _members.values().toArray(new L2ClanMember[_members.size()]);
	}

	public GArray<L2Player> getOnlineMembers(String exclude)
	{
		GArray<L2Player> result = new GArray<L2Player>();
		for(L2ClanMember temp : _members.values())
			if(temp.isOnline() && (exclude == null || !temp.getName().equals(exclude)))
				result.add(temp.getPlayer());

		return result;
	}

	public int getAllyId()
	{
		return _allyId;
	}

	public byte getLevel()
	{
		return _level;
	}

	/**
	 * Возвращает замок, которым владеет клан
	 *
	 * @return ID замка
	 */
	public int getHasCastle()
	{
		return _hasCastle;
	}

	/**
	 * Возвращает крепость, которой владеет клан
	 *
	 * @return ID крепости
	 */
	public int getHasFortress()
	{
		return _hasFortress;
	}

	public int getHasHideout()
	{
		return _hasHideout;
	}

	/**
	 * 
	 * @param type 1 =clanHall, 2= castle, 3 = fortress
	 * @return true if have
	 */
	public boolean getHasUnit(int type)
	{
		return  type == 1 ? _hasHideout > 0 : type == 2 ? _hasCastle > 0 : type == 3 && _hasFortress > 0;
	}

	public void setAllyId(int allyId)
	{
		_allyId = allyId;
	}

	/**
	 * Устанавливает замок, которым владеет клан.<BR>
	 * Одновременно владеть и замком и крепостью нельзя
	 *
	 * @param castle ID замка
	 */
	public void setHasCastle(int castle)
	{
		if(_hasFortress != 0)
			_hasFortress = 0;

		_hasCastle = castle;
	}

	/**
	 * Устанавливает крепость, которой владеет клан.<BR>
	 * Одновременно владеть и крепостью и замком нельзя
	 *
	 * @param fortress ID крепости
	 */
	public void setHasFortress(int fortress)
	{
		if(_hasCastle == 0)
			_hasFortress = fortress;
	}

	public void setHasHideout(int hasHideout)
	{
		_hasHideout = hasHideout;
	}

	public void setLevel(byte level)
	{
		_level = level;
	}

	public boolean isMember(Integer id)
	{
		return _members.containsKey(id);
	}

	public void updateClanleader(int leaderId, Connection con) throws SQLException
	{
		PreparedStatement stmt2;

		if(getLeaderId() == 0)
		{
			_log.warn("updateClanInDB with empty LeaderId");
			Thread.dumpStack();
			return;
		}
		if(getClanId() == 0)
		{
			_log.warn("updateClanInDB with empty ClanId");
			Thread.dumpStack();
			return;
		}

		stmt2 = con.prepareStatement("UPDATE clan_data SET leader_id = ? WHERE clan_id = ?");
		stmt2.setInt(1, leaderId);
		stmt2.setInt(2, getClanId());
		stmt2.executeUpdate();
		stmt2.close();

		L2ClanMember member = getClanMember(leaderId);
		L2ClanMember leader = getLeader();
		L2Player player = leader.getPlayer();
		if(leader.isOnline())
		{
			player.setClanLeader(false);
			L2ItemInstance item = player.getInventory().getItemByItemId(6841);
			if(item != null && item.isEquipped())
				player.getInventory().unEquipItemAndSendChanges(item);
		}
		setLeader(member);
		if(member.isOnline())
			member.getPlayer().setClanLeader(true);
	}

	public void updateClanInDB()
	{
		if(getLeaderId() == 0)
		{
			_log.warn("updateClanInDB with empty LeaderId");
			Thread.dumpStack();
			return;
		}

		if(getClanId() == 0)
		{
			_log.warn("updateClanInDB with empty ClanId");
			Thread.dumpStack();
			return;
		}
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET leader_id=?,ally_id=?,reputation_score=?,expelled_member=?,leaved_ally=?,dissolved_ally=?,clan_level=?,airship=? WHERE clan_id=?");
			statement.setInt(1, getLeaderId());
			statement.setInt(2, getAllyId());
			statement.setInt(3, getReputationScore());
			statement.setLong(4, getExpelledMemberTime() / 1000);
			statement.setLong(5, getLeavedAllyTime() / 1000);
			statement.setLong(6, getDissolvedAllyTime() / 1000);
			statement.setInt(7, _level);
			statement.setInt(8, _airshipEp);
			statement.setInt(9, getClanId());
			statement.execute();

			if(Config.DEBUG)
				_log.debug("Clan data saved in db: " + getClanId());
		}
		catch(Exception e)
		{
			_log.warn("error while updating clan '" + getClanId() + "' data in db: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void store()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,hasFortress,hasHideout,ally_id,leader_id,expelled_member,leaved_ally,dissolved_ally) values (?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _clanId);
			statement.setString(2, _name);
			statement.setInt(3, _level);
			statement.setInt(4, _hasCastle);
			statement.setInt(5, _hasFortress);
			statement.setInt(6, _hasHideout);
			statement.setInt(7, _allyId);
			statement.setInt(8, getLeaderId());
			statement.setLong(9, getExpelledMemberTime() / 1000);
			statement.setLong(10, getLeavedAllyTime() / 1000);
			statement.setLong(11, getDissolvedAllyTime() / 1000);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("UPDATE characters SET clanid=?,pledge_type=0 WHERE obj_Id=?");
			statement.setInt(1, getClanId());
			statement.setInt(2, getLeaderId());
			statement.execute();

			if(Config.DEBUG)
				_log.debug("New clan saved in db: " + getClanId());
		}
		catch(Exception e)
		{
			_log.warn("error while saving new clan to db " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void removeMemberInDatabase(L2ClanMember member)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET clanid=0, pledge_type=0, pledge_rank=0, lvl_joined_academy=0, apprentice=0, title='', leaveclan=? WHERE obj_Id=?");
			statement.setLong(1, System.currentTimeMillis() / 1000);
			statement.setInt(2, member.getObjectId());
			statement.execute();

			if(Config.DEBUG)
				_log.debug("clan member removed in db: " + getClanId());
		}
		catch(Exception e)
		{
			_log.warn("error while removing clan member in db " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public static L2Clan restore(int clanId)
	{
		if(clanId == 0) // no clan
			return null;

		L2Clan clan = null;
		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		ResultSet clanData = null, clanMembers = null;
		try
		{
			L2ClanMember member;

			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clan_name,clan_level,hasCastle,hasFortress,hasHideout,ally_id,leader_id,reputation_score,expelled_member,leaved_ally,dissolved_ally,auction_bid_at,auction_cancel_time,airship FROM clan_data where clan_id=?");
			statement2 = con.prepareStatement(//
					"SELECT `c`.`char_name` AS `char_name`," + //
							"`s`.`level` AS `level`," + //
							"`s`.`class_id` AS `classid`," + //
							"`c`.`obj_Id` AS `obj_id`," + //
							"`c`.`title` AS `title`," + //
							"`c`.`pledge_type` AS `pledge_type`," + //
							"`c`.`pledge_rank` AS `pledge_rank`," + //
							"`c`.`apprentice` AS `apprentice` " + //
							"FROM `characters` `c` " + //
							"LEFT JOIN `character_subclasses` `s` ON (`s`.`char_obj_id` = `c`.`obj_Id` AND `s`.`isBase` = '1') " + //
							"WHERE `c`.`clanid`=? ORDER BY `c`.`lastaccess` DESC");

			statement.setInt(1, clanId);
			clanData = statement.executeQuery();

			if(clanData.next())
			{
				clan = new L2Clan(clanId);
				clan.setName(clanData.getString("clan_name"));
				clan.setLevel(clanData.getByte("clan_level"));
				clan.setHasCastle(clanData.getByte("hasCastle"));
				clan.setHasFortress(clanData.getByte("hasFortress"));
				clan.setHasHideout(clanData.getInt("hasHideout"));
				clan.setAllyId(clanData.getInt("ally_id"));
				clan._reputation = clanData.getInt("reputation_score");
				clan._auctionBiddedAt = clanData.getInt("auction_bid_at");
				clan._auctionCancelTime = clanData.getLong("auction_cancel_time");
				clan._airshipEp = clanData.getInt("airship");
				clan.setExpelledMemberTime(clanData.getLong("expelled_member") * 1000);
				clan.setLeavedAllyTime(clanData.getLong("leaved_ally") * 1000);
				clan.setDissolvedAllyTime(clanData.getLong("dissolved_ally") * 1000);
				int leaderId = clanData.getInt("leader_id");

				statement2.setInt(1, clan.getClanId());
				clanMembers = statement2.executeQuery();

				while(clanMembers.next())
				{
					member = new L2ClanMember(clan, clanMembers.getString("char_name"), clanMembers.getString("title"), clanMembers.getInt("level"), clanMembers.getInt("classid"), clanMembers.getInt("obj_id"), clanMembers.getInt("pledge_type"), clanMembers.getInt("pledge_rank"), clanMembers.getInt("apprentice"), clanMembers.getInt("obj_id") == leaderId, L2Player.loadVariables(clanMembers.getInt("obj_id")));
					if(member.getObjectId() == leaderId)
						clan.setLeader(member);
					else
						clan.addClanMember(member);
				}
				DbUtils.closeQuietly(clanMembers);
			}
			else
			{
				_log.debug("L2Clan.java clan " + clanId + " does't exist");
				return null;
			}

			if(clan.getName() == null)
				_log.warn("null name for clan?? " + clanId);

			clan.restoreSkills();
			clan.restoreSubPledges();
			clan.restoreRankPrivs();
			clan.setCrestId(CrestCache.getPledgeCrestId(clanId));
			clan.setCrestLargeId(CrestCache.getPledgeCrestLargeId(clanId));
		}
		catch(Exception e)
		{
			_log.warn("error while restoring clan " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(statement2, clanMembers);
			DbUtils.closeQuietly(con, statement, clanData);
		}
		return clan;
	}

	public void broadcastToOnlineMembers(L2GameServerPacket packet)
	{
		for(L2ClanMember member : _members.values())
			if(member.isOnline())
				member.getPlayer().sendPacket(packet);
	}

	public void broadcastToOtherOnlineMembers(L2GameServerPacket packet, L2Player player)
	{
		for(L2ClanMember member : _members.values())
			if(member.isOnline() && member.getPlayer() != player)
				member.getPlayer().sendPacket(packet);
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public void setCrestId(int newcrest)
	{
		_crestId = newcrest;
	}

	public int getCrestId()
	{
		return _crestId;
	}

	public boolean hasCrest()
	{
		return _crestId > 0;
	}

	public int getCrestLargeId()
	{
		return _crestLargeId;
	}

	public void setCrestLargeId(int newcrest)
	{
		_crestLargeId = newcrest;
	}

	public boolean hasCrestLarge()
	{
		return _crestLargeId > 0;
	}

	public int getAllyCrestId()
	{
		if(getAlliance() != null)
			return getAlliance().getAllyCrestId();
		return 0;
	}

	public ClanWarehouse getWarehouse()
	{
		return _warehouse;
	}

	public int getHiredGuards()
	{
		return _hiredGuards;
	}

	public void incrementHiredGuards()
	{
		_hiredGuards++;
	}

	public int isAtWar()
	{
		if(_atWarWith != null && _atWarWith.size() > 0)
			return 1;
		return 0;
	}

	public int isAtWarOrUnderAttack()
	{
		if(_atWarWith != null && _atWarWith.size() > 0 || _underAttackFrom != null && _underAttackFrom.size() > 0)
			return 1;
		return 0;
	}

	public boolean isAtWarWith(Integer id)
	{
		L2Clan clan = ClanTable.getInstance().getClan(id);
		if(_atWarWith != null && _atWarWith.size() > 0)
			if(_atWarWith.contains(clan))
				return true;
		return false;
	}

	public boolean isUnderAttackFrom(Integer id)
	{
		L2Clan clan = ClanTable.getInstance().getClan(id);
		if(_underAttackFrom != null && _underAttackFrom.size() > 0)
			if(_underAttackFrom.contains(clan))
				return true;
		return false;
	}

	public void setEnemyClan(L2Clan clan)
	{
		//Integer id = clan.getClanId();
		_atWarWith.add(clan);
	}

	public void setEnemyClan(Integer clan)
	{
		_log.warn("setEnemyClan");
		L2Clan Clan = ClanTable.getInstance().getClan(clan);
		_atWarWith.add(Clan);
	}

	public void deleteEnemyClan(L2Clan clan)
	{
		//Integer id = clan.getClanId();
		_atWarWith.remove(clan);
	}

	// clans that are attacking this clan
	public void setAttackerClan(L2Clan clan)
	{
		//int id = clan.getClanId();
		_underAttackFrom.add(clan);
	}

	public void setAttackerClan(Integer clan)
	{
		L2Clan Clan = ClanTable.getInstance().getClan(clan);
		_underAttackFrom.add(Clan);
	}

	public void deleteAttackerClan(L2Clan clan)
	{
		//Integer id = clan.getClanId();
		_underAttackFrom.remove(clan);
	}

	public List<L2Clan> getEnemyClans()
	{
		return _atWarWith;
	}

	public int getWarsCount()
	{
		return _atWarWith.size();
	}

	public List<L2Clan> getAttackerClans()
	{
		return _underAttackFrom;
	}

	public L2Alliance getAlliance()
	{
		return _allyId == 0 ? null : ClanTable.getInstance().getAlliance(_allyId);
	}

	public void setExpelledMemberTime(long time)
	{
		_expelledMemberTime = time;
	}

	public long getExpelledMemberTime()
	{
		return _expelledMemberTime;
	}

	public void setExpelledMember()
	{
		_expelledMemberTime = System.currentTimeMillis();
		updateClanInDB();
	}

	public void setLeavedAllyTime(long time)
	{
		_leavedAllyTime = time;
	}

	public long getLeavedAllyTime()
	{
		return _leavedAllyTime;
	}

	public void setLeavedAlly()
	{
		_leavedAllyTime = System.currentTimeMillis();
		updateClanInDB();
	}

	public void setDissolvedAllyTime(long time)
	{
		_dissolvedAllyTime = time;
	}

	public long getDissolvedAllyTime()
	{
		return _dissolvedAllyTime;
	}

	public void setDissolvedAlly()
	{
		_dissolvedAllyTime = System.currentTimeMillis();
		updateClanInDB();
	}

	public boolean canInvite()
	{
		return System.currentTimeMillis() - _expelledMemberTime >= EXPELLED_MEMBER_PENALTY;
	}

	public boolean canJoinAlly()
	{
		return System.currentTimeMillis() - _leavedAllyTime >= LEAVED_ALLY_PENALTY;
	}

	public boolean canCreateAlly()
	{
		return System.currentTimeMillis() - _dissolvedAllyTime >= DISSOLVED_ALLY_PENALTY;
	}

	public int getRank()
	{
		L2Clan[] clans = ClanTable.getInstance().getClans();
		Arrays.sort(clans, REPUTATION_COMPARATOR);

		int place = 1;
		for(int i = 0; i < clans.length; i++)
		{
			if(i == REPUTATION_PLACES)
				return 0;

			L2Clan clan = clans[i];
			if(clan == this)
				return place + i;
		}

		return 0;
	}

	public int getReputationScore()
	{
		return _reputation;
	}

	public void setReputationScore(int rep)
	{
		if(_reputation >= 0 && rep < 0)
		{
			broadcastToOnlineMembers(Msg.SINCE_THE_CLAN_REPUTATION_SCORE_HAS_DROPPED_TO_0_OR_LOWER_YOUR_CLAN_SKILLS_WILL_BE_DE_ACTIVATED);
			L2Skill[] skills = getAllSkills();
			for(L2ClanMember member : _members.values())
				if(member.isOnline())
				{
					for(L2Skill sk : skills)
						member.getPlayer().removeSkill(sk, false);

					if(_subPledgeSkills.containsKey(member.getPledgeType()))
						for(L2Skill sk : _subPledgeSkills.get(member.getPledgeType()))
							member.getPlayer().removeSkill(sk, false);
					member.getPlayer().sendChanges();
				}

		}
		else if(_reputation < 0 && rep >= 0)
		{
			broadcastToOnlineMembers(Msg.THE_CLAN_SKILL_WILL_BE_ACTIVATED_BECAUSE_THE_CLANS_REPUTATION_SCORE_HAS_REACHED_TO_0_OR_HIGHER);
			L2Skill[] skills = getAllSkills();
			for(L2ClanMember member : _members.values())
				if(member.isOnline())
				{
					for(L2Skill sk : skills)
					{
						member.getPlayer().sendPacket(new PledgeSkillListAdd(sk.getId(), sk.getLevel()));
						if(sk.getMinPledgeClass() <= member.getPlayer().getPledgeRank())
							member.getPlayer().addSkill(sk, false);
					}

					if(_subPledgeSkills.containsKey(member.getPledgeType()))
						for(L2Skill sk : _subPledgeSkills.get(member.getPledgeType()))
							member.getPlayer().addSkill(sk, false);

					member.getPlayer().sendChanges();
				}
		}

		_reputation = rep;

		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
		updateClanInDB();
	}

	public int incReputation(int inc, boolean Rate, String source)
	{
		if(_level < 5)
		{
			_log.warn("Trying to gauge clan reputation for clan below 5 lvl. CRP: " + inc + " source: " + source);
			Thread.dumpStack();
			return 0;
		}

		if(Rate && Math.abs(inc) <= Config.RATE_CLAN_REP_SCORE_MAX_AFFECTED)
			inc = Math.round(inc * Config.RATE_CLAN_REP_SCORE);

		setReputationScore(_reputation + inc);
		logClanRep.info(_name + "{" + _clanId + "} " + source + ": add: " + inc + " total: " + _reputation);

		return inc;
	}

	/* ============================ clan skills stuff ============================ */

	private void restoreSkills()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			// Retrieve all skills of this L2Player from the database
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT skill_id,pledge_id,skill_level FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, getClanId());
			rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while(rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");
				int pledgeId = rset.getInt("pledge_id");
				// Create a L2Skill object for each record
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				// Add the L2Skill object to the L2Clan _skills
				if(pledgeId >= 0)
				{
					GArray<L2Skill> skills = _subPledgeSkills.get(pledgeId);

					if(skills == null)
						skills = new GArray<L2Skill>();

					skills.add(skill);
					_subPledgeSkills.put(pledgeId, skills);
				}
				else
					_skills.put(skill.getId(), skill);
			}
		}
		catch(Exception e)
		{
			_log.warn("Could not restore clan skills: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	/**
	 * used to retrieve all skills
	 */
	public final L2Skill[] getAllSkills()
	{
		if(_reputation < 0)
			return new L2Skill[0];

		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}

	public GArray<L2Skill> getSubPledgeSkills(int subPledge)
	{
		if(_subPledgeSkills.containsKey(subPledge))
			return _subPledgeSkills.get(subPledge);

		return null;
	}

	/**
	 * used to add a new skill to the list, send a packet to all online clan members, update their stats and store it in db
	 */
	public void addNewSkill(L2Skill newSkill, L2Player player, int subPledge)
	{
		Connection con = null;
		PreparedStatement statement = null;
		if(newSkill != null)
		{
			//int pledgeId = SkillTreeTable.getInstance().isSubPledgeSkill(newSkill.getId()) && player != null ? getSubPledgeIdByLeader(player) : -1;
			// Replace oldSkill by newSkill or Add the newSkill
			if(subPledge >= 0)
			{
				GArray<L2Skill> skills = _subPledgeSkills.get(subPledge);
				if(skills == null)
					skills = new GArray<>();

				for(int i = 0; i < skills.size(); i++)
					if(skills.get(i).getId() == newSkill.getId())
						skills.remove(i);

				skills.add(newSkill);

				_subPledgeSkills.put(subPledge, skills);
			}
			else
				_skills.put(newSkill.getId(), newSkill);

			try
			{
				con = DatabaseFactory.getInstance().getConnection();

				statement = con.prepareStatement("REPLACE INTO clan_skills (clan_id,pledge_id,skill_id,skill_level,skill_name) VALUES (?,?,?,?,?)");
				statement.setInt(1, getClanId());
				statement.setInt(2, subPledge);
				statement.setInt(3, newSkill.getId());
				statement.setInt(4, newSkill.getLevel());
				statement.setString(5, newSkill.getName());
				statement.execute();
			}
			catch(Exception e)
			{
				_log.warn("Error could not store char skills: " + e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}

			for(L2ClanMember temp : _members.values())
				if(temp.isOnline() && temp.getPlayer() != null)
				{
					boolean add = false;
					if(subPledge >= 0)
					{
						temp.getPlayer().sendPacket(new ExSubPledgetSkillAdd(newSkill.getId(), newSkill.getLevel(), subPledge));
						if(temp.getPlayer().getPledgeType() == subPledge)
						{
							temp.getPlayer().addSkill(newSkill, false);
							add = true;
						}
					}
					else if(subPledge < 0)
					{
						temp.getPlayer().sendPacket(new PledgeSkillListAdd(newSkill.getId(), newSkill.getLevel()));
						if(newSkill.getMinPledgeClass() <= temp.getPlayer().getPledgeRank())
						{
							temp.getPlayer().addSkill(newSkill, false);
							add = true;
						}
					}
					if(add)
					{
						if(temp.getPlayer() != player)
							temp.getPlayer().sendPacket(new SkillList(temp.getPlayer()));
						temp.getPlayer().sendChanges();
					}
				}
		}
	}

	public void addAndShowSkillsToPlayer(L2Player player)
	{
		if(_reputation < 0)
			return;

		player.sendPacket(new PledgeSkillList(player));

		for(L2Skill s : _skills.values())
		{
			if(s == null)
				continue;
			player.sendPacket(new PledgeSkillListAdd(s.getId(), s.getLevel()));
			if(s.getMinPledgeClass() <= player.getPledgeRank())
				player.addSkill(s, false);
		}

		if(player.getPledgeType() >= 0 && _subPledgeSkills.containsKey(player.getPledgeType()))
			for(L2Skill skill : _subPledgeSkills.get(player.getPledgeType()))
				player.addSkill(skill, false);

		if(_hasCastle > 0 && ResidenceManager.getInstance().getBuildingById(_hasCastle) != null)
		{
			ResidenceManager.getInstance().getBuildingById(_hasCastle).giveSkills(player);
			if(!TerritoryWarManager.getWar().isInProgress())
				TerritoryWarManager.getTerritoryById(_hasCastle + 80).giveSkills(player);
		}
		if(_hasFortress > 0 && ResidenceManager.getInstance().getBuildingById(_hasFortress) != null)
			ResidenceManager.getInstance().getBuildingById(_hasFortress).giveSkills(player);

		player.sendPacket(new SkillList(player));
	}

	public void removeClanSkills(L2Player player)
	{
		if(player == null)
			return;

		for(L2Skill sk : getAllSkills())
			player.removeSkill(sk, false);

		if(_subPledgeSkills.containsKey(player.getPledgeType()))
			for(L2Skill sk : _subPledgeSkills.get(player.getPledgeType()))
				player.removeSkill(sk, false);

		player.sendPacket(new SkillList(player));
	}

	/* ============================ clan subpledges stuff ============================ */

	public class SubPledge
	{
		private int _type;
		private int _leaderId;
		private String _name;

		public SubPledge(int type, int leaderId, String name)
		{
			_type = type;
			_leaderId = leaderId;
			_name = name;
		}

		public int getType()
		{
			return _type;
		}

		public String getName()
		{
			return _name;
		}

		public int getLeaderId()
		{
			return _leaderId;
		}

		public void setLeaderId(int leaderId)
		{
			_leaderId = leaderId;
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE clan_subpledges SET leader_id=? WHERE clan_id=? and type=?");
				statement.setInt(1, _leaderId);
				statement.setInt(2, getClanId());
				statement.setInt(3, _type);
				statement.execute();
			}
			catch(Exception e)
			{
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}

		public String getLeaderName()
		{
			for(L2ClanMember member : _members.values())
				if(member.getObjectId() == _leaderId)
					return member.getName();
			return "";
		}
	}

	public final boolean isAcademy(int pledgeType)
	{
		return pledgeType == SUBUNIT_ACADEMY;
	}

	public final boolean isRoyalGuard(int pledgeType)
	{
		return pledgeType == SUBUNIT_ROYAL1 || pledgeType == SUBUNIT_ROYAL2;
	}

	public final boolean isOrderOfKnights(int pledgeType)
	{
		return pledgeType == SUBUNIT_KNIGHT1 || pledgeType == SUBUNIT_KNIGHT2 || pledgeType == SUBUNIT_KNIGHT3 || pledgeType == SUBUNIT_KNIGHT4;
	}

	public int getAffiliationRank(int pledgeType)
	{
		if(isAcademy(pledgeType))
			return 9;
		else if(isOrderOfKnights(pledgeType))
			return 8;
		else if(isRoyalGuard(pledgeType))
			return 7;
		else
			return 6;
	}

	public final SubPledge getSubPledge(int pledgeType)
	{
		if(_subPledges == null)
			return null;

		return _subPledges.get(pledgeType);
	}

	public final void addSubPledge(SubPledge sp, boolean updateDb)
	{
		_subPledges.put(sp.getType(), sp);

		if(updateDb)
		{
			broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(sp));
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("INSERT INTO `clan_subpledges` (clan_id,type,leader_id,name) VALUES (?,?,?,?)");
				statement.setInt(1, getClanId());
				statement.setInt(2, sp.getType());
				statement.setInt(3, sp.getLeaderId());
				statement.setString(4, sp.getName());
				statement.execute();
			}
			catch(Exception e)
			{
				_log.warn("Could not store clan Sub pledges: " + e);
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	public int createSubPledge(L2Player player, int pledgeType, int leaderId, String name)
	{
		int temp = pledgeType;
		pledgeType = getAvailablePledgeTypes(pledgeType);

		if(pledgeType == SUBUNIT_NONE)
		{
			if(temp == SUBUNIT_ACADEMY)
				player.sendPacket(Msg.YOUR_CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY);
			else
				player.sendMessage("You can't create any more sub-units of this type");
			return SUBUNIT_NONE;
		}

		switch(pledgeType)
		{
			case SUBUNIT_ACADEMY:
				break;
			case SUBUNIT_ROYAL1:
			case SUBUNIT_ROYAL2:
				if(getReputationScore() < 5000)
				{
					player.sendPacket(Msg.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
					return SUBUNIT_NONE;
				}
				incReputation(-5000, false, "SubunitCreate");
				break;
			case SUBUNIT_KNIGHT1:
			case SUBUNIT_KNIGHT2:
			case SUBUNIT_KNIGHT3:
			case SUBUNIT_KNIGHT4:
				if(getReputationScore() < 10000)
				{
					player.sendPacket(Msg.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
					return SUBUNIT_NONE;
				}
				incReputation(-10000, false, "SubunitCreate");
				break;
		}

		addSubPledge(new SubPledge(pledgeType, leaderId, name), true);
		return pledgeType;
	}

	public int getAvailablePledgeTypes(int pledgeType)
	{
		if(pledgeType == SUBUNIT_NONE)
			return SUBUNIT_NONE;

		if(_subPledges.get(pledgeType) != null)
			switch(pledgeType)
			{
				case SUBUNIT_ACADEMY:
					return 0;
				case SUBUNIT_ROYAL1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_ROYAL2);
					break;
				case SUBUNIT_ROYAL2:
					return 0;
				case SUBUNIT_KNIGHT1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT2);
					break;
				case SUBUNIT_KNIGHT2:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT3);
					break;
				case SUBUNIT_KNIGHT3:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT4);
					break;
				case SUBUNIT_KNIGHT4:
					return 0;
			}
		return pledgeType;
	}

	private void restoreSubPledges()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, getClanId());
			rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while(rset.next())
			{
				int type = rset.getInt("type");
				int leaderId = rset.getInt("leader_id");
				String name = rset.getString("name");
				SubPledge pledge = new SubPledge(type, leaderId, name);
				addSubPledge(pledge, false);
			}
		}
		catch(Exception e)
		{
			_log.warn("Could not restore clan SubPledges: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	/**
	 * used to retrieve all subPledges
	 */
	public final SubPledge[] getAllSubPledges()
	{
		return _subPledges.values().toArray(new SubPledge[_subPledges.values().size()]);
	}

	public int getSubPledgeLimit(int pledgeType)
	{
		int limit;
		switch(_level)
		{
			case 0:
				limit = 10;
				break;
			case 1:
				limit = 15;
				break;
			case 2:
				limit = 20;
				break;
			case 3:
				limit = 30;
				break;
			default:
				limit = 40;
				break;
		}
		switch(pledgeType)
		{
			case SUBUNIT_ACADEMY:
				limit = 20;
				break;
			case SUBUNIT_ROYAL1:
			case SUBUNIT_ROYAL2:
				if(_level > 10)
					limit = 30;
				else
					limit = 20;
				break;
			case SUBUNIT_KNIGHT1:
			case SUBUNIT_KNIGHT2:
				if(getLevel() > 8)
					limit = 25;
				else
					limit = 10;
				break;
			case SUBUNIT_KNIGHT3:
			case SUBUNIT_KNIGHT4:
				if(getLevel() > 9)
					limit = 25;
				else
					limit = 10;
				break;
		}
		return limit;
	}

	public int getSubPledgeMembersCount(int pledgeType)
	{
		int result = 0;
		for(L2ClanMember temp : _members.values())
			if(temp.getPledgeType() == pledgeType)
				result++;
		return result;
	}

	public int getSubPledgeLeaderId(int pledgeType)
	{
		if(_subPledges.containsKey(pledgeType))
			return _subPledges.get(pledgeType).getLeaderId();

		return 0;
	}

	/* ============================ clan privilege ranks stuff ============================ */

	public class RankPrivs
	{
		private int _rank;
		private int _party;
		private int _privs;

		public RankPrivs(int rank, int party, int privs)
		{
			_rank = rank;
			_party = party;
			_privs = privs;
		}

		public int getRank()
		{
			return _rank;
		}

		public int getParty()
		{
			return _party;
		}

		public void setParty(int party)
		{
			_party = party;
		}

		public int getPrivs()
		{
			return _privs;
		}

		public void setPrivs(int privs)
		{
			_privs = privs;
		}
	}

	private void restoreRankPrivs()
	{
		if(_privs == null)
			InitializePrivs();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			// Retrieve all skills of this L2Player from the database
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT privilleges, rank FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, getClanId());
			rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while(rset.next())
			{
				int rank = rset.getInt("rank");
				//int party = rset.getInt("party"); - unused?
				int privileges = rset.getInt("privilleges");
				//noinspection ConstantConditions
				RankPrivs p = _privs.get(rank);
				if(p != null)
				{
					p.setPrivs(privileges);
					p.setParty(countMembersByRank(rank));
				}
				else
					_log.warn("Invalid rank value (" + rank + "), please check clan_privs table");
			}
		}
		catch(Exception e)
		{
			_log.warn("Could not restore clan privs by rank: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void InitializePrivs()
	{
		for(int i = 1; i < 10; i++)
			_privs.put(i, new RankPrivs(i, 0, CP_NOTHING));
	}

	public void updatePrivsForRank(int rank)
	{
		for(L2ClanMember member : _members.values())
			if(member.isOnline() && member.getPlayer() != null && member.getPlayer().getPowerGrade() == rank)
			{
				if(member.getPlayer().isClanLeader())
					continue;
				member.getPlayer().sendUserInfo(false);
			}
	}

	public RankPrivs getRankPrivs(int rank)
	{
		/*int priv = 0;

		 for(RankPrivs rp : _privs.values())
		 if(rp._rank <= rank)
		 priv |= rank;

		 return priv;*/
		if(rank < 1 || rank > 9)
			return null;
		if(_privs.get(rank) == null)
			setRankPrivs(rank, 0);
		return _privs.get(rank);
	}

	public void setRankPrivs(int rank, int privs)
	{
		if(rank < 1 || rank > 9)
			return;

		if(_privs.get(rank) != null)
			_privs.get(rank).setPrivs(privs);
		else
			_privs.put(rank, new RankPrivs(rank, 0, privs));

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			//_log.warn("requested store clan privs in db for rank: " + rank + ", privs: " + privs);
			// Retrieve all skills of this L2Player from the database
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO clan_privs (clan_id,rank,privilleges) VALUES (?,?,?)");
			statement.setInt(1, getClanId());
			statement.setInt(2, rank);
			statement.setInt(3, privs);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("Could not store clan privs for rank: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public int countMembersByRank(int rank)
	{
		int ret = 0;
		for(L2ClanMember m : getMembers())
			if(m.getPowerGrade() == rank)
				ret++;
		return ret;
	}

	/**
	 * used to retrieve all privilege ranks
	 */
	public final Collection<RankPrivs> getAllRankPrivs()
	{
		if(_privs == null)
			return new FastMap<Integer, RankPrivs>().values();

		return _privs.values();
	}

	private int _auctionBiddedAt = 0;
	private long _auctionCancelTime = 0;

	public int getAuctionBiddedAt()
	{
		return _auctionBiddedAt;
	}

	public long getAuctionCancelTime()
	{
		return _auctionCancelTime;
	}

	public void setAuctionBiddedAt(int id, long cancelTime)
	{
		_auctionBiddedAt = id;
		_auctionCancelTime = cancelTime;
		//store changes to DB
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET auction_bid_at = ?, auction_cancel_time = ? WHERE clan_id=?");
			statement.setInt(1, id);
			statement.setLong(2, cancelTime);
			statement.setInt(3, getClanId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("Could not store auction for clan: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void sendMessageToAll(String message)
	{
		for(L2ClanMember member : _members.values())
			if(member.isOnline() && member.getPlayer() != null)
				member.getPlayer().sendMessage(message);
	}

	public void sendMessageToAll(String message, String message_ru)
	{
		for(L2ClanMember member : _members.values())
			if(member.isOnline() && member.getPlayer() != null)
			{
				L2Player player = member.getPlayer();
				if(player.getVar("lang@") == null || player.getVar("lang@").equalsIgnoreCase("en") || message_ru.equals(""))
					player.sendMessage(message);
				else
					player.sendMessage(message_ru);
			}
	}

	private Siege _siege;
	private boolean _isDefender;
	private boolean _isAttacker;

	public void setSiege(Siege siege)
	{
		_siege = siege;
		_siegeDeath = 0;
		_siegeKills = 0;
	}

	public Siege getSiege()
	{
		return _siege;
	}

	public void setDefender(boolean b)
	{
		_isDefender = b;
	}

	public void setAttacker(boolean b)
	{
		_isAttacker = b;
	}

	public boolean isDefender()
	{
		return _isDefender;
	}

	public boolean isAttacker()
	{
		return _isAttacker;
	}

	public int getHaseBase()
	{
		return _hasCastle > 0 ? _hasCastle : _hasFortress;
	}

	private static class ClanReputationComparator implements Comparator<L2Clan>
	{
		public int compare(L2Clan o1, L2Clan o2)
		{
			return o2.getReputationScore() - o1.getReputationScore();
		}
	}

	public static void broadcastRelationsToOnlineMembers(L2Clan clan1, L2Clan clan2)
	   {
		   for(L2Player member : clan1.getOnlineMembers(""))
			   for(L2Player ap : L2World.getAroundPlayers(member))
				   if(clan2.getClanMember(ap.getName()) != null)
					   member.sendRelation(ap);
	   }

	public void notifyClanMembers(L2Player player, boolean online) throws Exception
	{
		if(getClanMember(player.getObjectId()) == null)
			return;

		if(online)
		{
			getClanMember(player.getObjectId()).setPlayerInstance(player);

			int sponsor = player.getSponsor();
			int apprentice = player.getApprentice();
			SystemMessage msg = new SystemMessage(SystemMessage.CLAN_MEMBER_S1_HAS_LOGGED_INTO_GAME).addString(player.getName());
			PledgeShowMemberListUpdate memberUpdate = new PledgeShowMemberListUpdate(player);
			player.sendPacket(memberUpdate);
			for(L2Player clanMember : getOnlineMembers(player.getName()))
			{
				clanMember.sendPacket(memberUpdate);
				if(clanMember.getObjectId() == sponsor)
					clanMember.sendPacket(new SystemMessage(SystemMessage.S1_YOUR_CLAN_ACADEMYS_APPRENTICE_HAS_LOGGED_IN).addString(player.getName()));
				else if(clanMember.getObjectId() == apprentice)
					clanMember.sendPacket(new SystemMessage(SystemMessage.S1_YOUR_CLAN_ACADEMYS_SPONSOR_HAS_LOGGED_IN).addString(player.getName()));
				else
					clanMember.sendPacket(msg);
			}
		}
		else
		{
			int sponsor = getClanMember(player.getObjectId()).getSponsor();
			int apprentice = player.getApprentice();
			PledgeShowMemberListUpdate memberUpdate = new PledgeShowMemberListUpdate(player);
			for(L2Player clanMember : getOnlineMembers(player.getName()))
			{
				if(clanMember.getObjectId() == player.getObjectId())
					continue;
				clanMember.sendPacket(memberUpdate);
				if(clanMember.getObjectId() == sponsor)
					clanMember.sendPacket(new SystemMessage(SystemMessage.S1_YOUR_CLAN_ACADEMYS_APPRENTICE_HAS_LOGGED_OUT).addString(_name));
				else if(clanMember.getObjectId() == apprentice)
					clanMember.sendPacket(new SystemMessage(SystemMessage.S1_YOUR_CLAN_ACADEMYS_SPONSOR_HAS_LOGGED_OUT).addString(_name));
			}
			getClanMember(player.getObjectId()).setPlayerInstance(null);
		}
	}

	public String getNotice()
	{
		return _notice;
	}

	/**
	 * Назначить новое сообщение
	 */
	public void setNotice(String notice)
	{
		_notice = notice;
	}

	public void incSiegeKills()
	{
		_siegeKills++;
	}

	public void incSiegeDeath()
	{
		_siegeDeath++;
	}

	public int getSiegeKills()
	{
		return _siegeKills;
	}

	public int getSiegeDeath()
	{
		return _siegeDeath;
	}

	public int getTerritoryId()
	{
		return _territoryId;
	}

	public void setTerritoryId(int terrId)
	{
		_territoryId = terrId;
		for(L2Player player : getOnlineMembers(""))
			player.setTerritoryId(terrId);
	}

	public void setCamp(L2NpcInstance camp)
	{
		_camp = camp;
	}

	public L2NpcInstance getCamp()
	{
		return _camp;
	}

	public void removeCamp()
	{
		if(_camp != null)
		{
			_camp.deleteMe();
			_camp = null;
		}
	}

	public void setAirship(L2ClanAirship cas)
	{
		if(cas == null && _airship != null)
		{
			_airshipEp = _airship.getCurrentEp();
			updateClanInDB();
		}
		else if(cas != null)
			cas.setCurrentEp(_airshipEp);

		_airship = cas;
	}

	public L2ClanAirship getAirship()
	{
		return _airship;
	}

	public boolean isAirshipEnabled()
	{
		return _airshipEp > -1;
	}

	public void registerAirshipLicense()
	{
		_airshipEp = 600;
		updateClanInDB();
	}

	@Override
	public int compareTo(L2Clan a)
	{
		return getName().compareTo(a.getName());
	}
}