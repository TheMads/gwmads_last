package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;
import ru.l2gw.gameserver.serverpackets.PledgeShowMemberListDeleteAll;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClanTable
{
	private static final Log _log = LogFactory.getLog(ClanTable.class.getName());

	private static ClanTable _instance;

	private final Map<Integer, L2Clan> _clans = new ConcurrentHashMap<>();
	private final Map<Integer, L2Alliance> _alliances = new ConcurrentHashMap<>();
	private final GArray<Integer> _npcPledgeId = new GArray<Integer>();

	public static ClanTable getInstance()
	{
		if(_instance == null)
			new ClanTable();
		return _instance;
	}

	public L2Clan[] getClans()
	{
		return _clans.values().toArray(new L2Clan[_clans.size()]);
	}

	public L2Alliance[] getAlliances()
	{
		return _alliances.values().toArray(new L2Alliance[_alliances.size()]);
	}

	private ClanTable()
	{
		_instance = this;
		restoreClans();
		restoreAllies();
		restoreWars();
	}

	public L2Clan getClan(final int clanId)
	{
		if(clanId <= 0)
			return null;

		return _clans.get(clanId);
	}

	public L2Alliance getAlliance(final int allyId)
	{
		if(allyId <= 0)
			return null;
		return _alliances.get(allyId);
	}

	public void restoreClans()
	{
		final GArray<Integer> clanIds = new GArray<Integer>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clan_id FROM clan_data");
			result = statement.executeQuery();
			while(result.next())
				clanIds.add(result.getInt("clan_id"));

			DbUtils.closeQuietly(statement, result);

			statement = con.prepareStatement("SELECT pledge_id FROM npc_crest");
			result = statement.executeQuery();
			while(result.next())
				_npcPledgeId.add(result.getInt("pledge_id"));
		}
		catch(final Exception e)
		{
			_log.warn("Error while restoring clans!!! " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, result);
		}

		for(final int clanId : clanIds)
		{
			final L2Clan clan = L2Clan.restore(clanId);

			if(clan == null)
			{
				_log.warn("Error while restoring clanId: " + clanId);
				continue;
			}

			if(clan.getMembersCount() <= 0)
			{
				_log.warn("membersCount = 0 for clanId: " + clanId + " " + clan.getName() + " clan deleted.");
				deleteClanFromDb(clanId);
				continue;
			}

			if(clan.getLeader() == null)
			{
				_log.warn("Not found leader for clanId: " + clanId + " " + clan.getName() + " set leader to: " + clan.getMembers()[0].getName());
				clan.setLeader(clan.getMembers()[0]);
				clan.updateClanInDB();
				continue;
			}

			_clans.put(clan.getClanId(), clan);//Sync not needed we touch this onload server only
		}

		_log.info("Loaded: " + _clans.size() + " clans.");
	}

	public void restoreAllies()
	{
		final GArray<Integer> allyIds = new GArray<Integer>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT ally_id FROM ally_data");
			result = statement.executeQuery();
			while(result.next())
				allyIds.add(result.getInt("ally_id"));
		}
		catch(final Exception e)
		{
			_log.warn("Error while restoring allies!!! " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, result);
		}

		for(final int allyId : allyIds)
		{
			final L2Alliance ally = new L2Alliance(allyId);

			if(ally.getMembersCount() <= 0)
			{
				_log.warn("membersCount = 0 for allyId: " + allyId);
				continue;
			}

			if(ally.getLeader() == null)
			{
				_log.warn("Not found leader for allyId: " + allyId);
				continue;
			}

			_alliances.put(ally.getAllyId(), ally);
		}

		_log.info("Loaded: " + _alliances.size() + " alliances.");
	}

	public L2Clan getClanByMemberId(final int id)
	{
			for(L2Clan clan : getClans())
				if(clan != null)
				{
					for(L2ClanMember member: clan.getMembers())
						if(member.getObjectId() == id)
							return clan;
				}

		return null;
	}

	public L2Clan getClanByName(final String clanName)
	{
		if(!StringUtil.isMatchingRegexp(clanName, Config.CLAN_NAME_TEMPLATE))
			return null;

		for(L2Clan clan : _clans.values())
		{
			if (clan.getName().equalsIgnoreCase(clanName))
				return clan;
		}

		return null;
	}

	public L2Alliance getAllyByName(final String allyName)
	{
		if(!StringUtil.isMatchingRegexp(allyName, Config.ALLY_NAME_TEMPLATE))
			return null;

		for(L2Alliance ally : _alliances.values())
		{
			if(ally.getAllyName().equalsIgnoreCase(allyName))
				return ally;
		}

		return null;
	}

	public L2Clan createClan(L2Player player, String clanName)
	{
		L2Clan clan = null;

		if(getClanByName(clanName) == null)
		{
			L2ClanMember leader = new L2ClanMember(player);
			leader.setPlayerInstance(player);
			clan = new L2Clan(IdFactory.getInstance().getNextId(), clanName, leader);
			clan.store();
			player.setClan(clan);
			player.setPowerGrade(6);
			_clans.put(clan.getClanId(), clan);
		}

		return clan;
	}

	public void dissolveClan(L2Player player)
	{
		L2Clan clan = player.getClan();
		long curtime = System.currentTimeMillis();
		SiegeManager.removeSiegeSkills(player);
		SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_RECENTLY_BEEN_DISMISSED_FROM_A_CLAN_YOU_ARE_NOT_ALLOWED_TO_JOIN_ANOTHER_CLAN_FOR_24_HOURS);
		for(L2ClanMember member : clan.getMembers())
		{
			L2Player clanMember = L2ObjectsStorage.getPlayer(member.getObjectId());
			if(clanMember != null)
			{
				clanMember.setClan(null);
				clanMember.setTitle(null);
				clanMember.sendPacket(new PledgeShowMemberListDeleteAll());
				clanMember.broadcastUserInfo(true);
				clanMember.sendPacket(sm);
				clanMember.setLeaveClanTime(curtime);
			}
		}
		clan.flush();
		deleteClanFromDb(clan.getClanId());
		_clans.remove(clan.getClanId());
		player.sendPacket(new SystemMessage(SystemMessage.CLAN_HAS_DISPERSED));
		player.setDeleteClanTime(curtime);
	}

	public void deleteClanFromDb(int clanId)
	{
		long curtime = System.currentTimeMillis();
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET clanid=0,title='',pledge_type=0,pledge_rank=0,lvl_joined_academy=0,apprentice=0,leaveclan=? WHERE clanid=?");
			statement.setLong(1, curtime / 1000);
			statement.setInt(2, clanId);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();

		}
		catch(Exception e)
		{
			_log.warn("could not dissolve clan:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public L2Alliance createAlliance(L2Player player, String allyName)
	{
		L2Alliance alliance = null;

		if(getAllyByName(allyName) == null)
		{
			L2Clan leader = player.getClan();
			alliance = new L2Alliance(IdFactory.getInstance().getNextId(), allyName, leader);
			alliance.store();
			_alliances.put(alliance.getAllyId(), alliance);

			leader.setAllyId(alliance.getAllyId());
			for(L2Player temp : leader.getOnlineMembers(null))
				temp.broadcastUserInfo(true);
			if(Config.DEBUG)
				_log.debug("New ally created: " + alliance.getAllyId() + " " + alliance.getAllyName());

		}

		return alliance;
	}

	public void dissolveAlly(L2Player player)
	{
		int allyId = player.getAllyId();
		SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_WITHDRAWN_FROM_THE_ALLIANCE);
		for(L2Clan clan : player.getAlliance().getMembers())
		{
			clan.setAllyId(0);
			clan.broadcastToOnlineMembers(sm);
			clan.setLeavedAlly();
			PledgeShowInfoUpdate pi = new PledgeShowInfoUpdate(clan);
			for(L2Player member : clan.getOnlineMembers(""))
			{
				member.sendPacket(pi);
				member.broadcastUserInfo(true);
			}
		}
		deleteAllyFromDb(allyId);
		_alliances.remove(allyId);
		player.sendPacket(new SystemMessage(SystemMessage.THE_ALLIANCE_HAS_BEEN_DISSOLVED));
		player.getClan().setDissolvedAlly();
	}

	public void deleteAllyFromDb(int allyId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET ally_id=0 WHERE ally_id=?");
			statement.setInt(1, allyId);
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM ally_data WHERE ally_id=?");
			statement.setInt(1, allyId);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("could not dissolve clan:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void startClanWar(L2Clan clan1, L2Clan clan2)
	{
		// clan1 is declaring war against clan2
		clan1.setEnemyClan(clan2);
		clan2.setAttackerClan(clan1);
		clan1.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan1));
		clan2.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan2));
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("REPLACE INTO clan_wars (clan1, clan2) VALUES(?,?)");
			statement.setInt(1, clan1.getClanId());
			statement.setInt(2, clan2.getClanId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("could not store clan war data:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		L2Clan.broadcastRelationsToOnlineMembers(clan1, clan2);
		L2Clan.broadcastRelationsToOnlineMembers(clan2, clan1);

		clan1.broadcastToOnlineMembers(new SystemMessage(SystemMessage.CLAN_WAR_HAS_BEEN_DECLARED_AGAINST_S1_CLAN_IF_YOU_ARE_KILLED_DURING_THE_CLAN_WAR_BY_MEMBERS_OF_THE_OPPOSING_CLAN_THE_EXPERIENCE_PENALTY_WILL_BE_REDUCED_TO_1_4_OF_NORMAL).addString(clan2.getName()));
		clan2.broadcastToOnlineMembers(new SystemMessage(SystemMessage.S1_CLAN_HAS_DECLARED_CLAN_WAR).addString(clan1.getName()));
	}

	public void stopClanWar(L2Clan clan1, L2Clan clan2)
	{
		// clan1 is ceases war against clan2
		clan1.deleteEnemyClan(clan2);
		clan2.deleteAttackerClan(clan1);

		clan1.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan1));
		clan2.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan2));
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
			statement.setInt(1, clan1.getClanId());
			statement.setInt(2, clan2.getClanId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("could not delete war data:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		L2Clan.broadcastRelationsToOnlineMembers(clan1, clan2);
		L2Clan.broadcastRelationsToOnlineMembers(clan2, clan1);

		clan1.broadcastToOnlineMembers(new SystemMessage(SystemMessage.THE_WAR_AGAINST_S1_CLAN_HAS_BEEN_STOPPED).addString(clan2.getName()));
		clan2.broadcastToOnlineMembers(new SystemMessage(SystemMessage.S1_CLAN_HAS_STOPPED_THE_WAR).addString(clan1.getName()));
	}

	private void restoreWars()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT clan1, clan2 FROM clan_wars");
			rset = statement.executeQuery();
			L2Clan clan1;
			L2Clan clan2;
			while(rset.next())
			{
				clan1 = getClan(rset.getInt("clan1"));
				clan2 = getClan(rset.getInt("clan2"));
				if(clan1 != null && clan2 != null)
				{
					clan1.setEnemyClan(clan2);
					clan2.setAttackerClan(clan1);
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("could not restore clan wars data:");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public boolean isNpcPledgeId(int clan_id)
	{
		return _npcPledgeId.contains(clan_id);
	}
}