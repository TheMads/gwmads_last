package ru.l2gw.gameserver.model;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.cache.CrestCache;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.tables.ClanTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

public class L2Alliance
{
	private static final Log _log = LogFactory.getLog(L2Alliance.class.getName());

	private String _allyName;
	private int _allyId;
	private L2Clan _leader = null;
	private Map<Integer, L2Clan> _members = new FastMap<Integer, L2Clan>();

	private int _allyCrestId;

	private long _expelledMemberTime;

	public static long EXPELLED_MEMBER_PENALTY = 24 * 60 * 60 * 1000;

	public L2Alliance(int allyId)
	{
		_allyId = allyId;
		restore();
	}

	public L2Alliance(int allyId, String allyName, L2Clan leader)
	{
		_allyId = allyId;
		_allyName = allyName;
		setLeader(leader);
	}

	public int getLeaderId()
	{
		return _leader != null ? _leader.getClanId() : 0;
	}

	public L2Clan getLeader()
	{
		return _leader;
	}

	public void setLeader(L2Clan leader)
	{
		_leader = leader;
		_members.put(leader.getClanId(), leader);
	}

	public String getAllyLeaderName()
	{
		return _leader != null ? _leader.getLeaderName() : "";
	}

	public void addAllyMember(L2Clan member, boolean storeInDb)
	{
		_members.put(member.getClanId(), member);

		if(storeInDb)
			storeNewMemberInDatabase(member);
	}

	public L2Clan getAllyMember(int id)
	{
		return _members.get(id);
	}

	public void removeAllyMember(int id)
	{
		if(_leader != null && _leader.getClanId() == id)
			return;
		L2Clan exMember = _members.remove(id);
		if(exMember == null)
		{
			_log.warn("Clan " + id + " not found in alliance while trying to remove");
			return;
		}
		removeMemberInDatabase(exMember);
	}

	public L2Clan[] getMembers()
	{
		return _members.values().toArray(new L2Clan[_members.size()]);
	}

	public int getMembersCount()
	{
		return _members.size();
	}

	public int getAllyId()
	{
		return _allyId;
	}

	public String getAllyName()
	{
		return _allyName;
	}

	public void setAllyCrestId(int allyCrestId)
	{
		_allyCrestId = allyCrestId;
	}

	public int getAllyCrestId()
	{
		return _allyCrestId;
	}

	public void setAllyId(int allyId)
	{
		_allyId = allyId;
	}

	public void setAllyName(String allyName)
	{
		_allyName = allyName;
	}

	public boolean isMember(int id)
	{
		return _members.containsKey(id);
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
		updateAllyInDB();
	}

	public boolean canInvite()
	{
		return System.currentTimeMillis() - _expelledMemberTime >= EXPELLED_MEMBER_PENALTY;
	}

	public void updateAllyInDB()
	{
		if(getLeaderId() == 0)
		{
			_log.warn("updateAllyInDB with empty LeaderId");
			Thread.dumpStack();
			return;
		}

		if(getAllyId() == 0)
		{
			_log.warn("updateAllyInDB with empty AllyId");
			Thread.dumpStack();
			return;
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE ally_data SET leader_id=?,expelled_member=? WHERE ally_id=?");
			statement.setInt(1, getLeaderId());
			statement.setLong(2, getExpelledMemberTime() / 1000);
			statement.setInt(3, getAllyId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("error while updating ally '" + getAllyId() + "' data in db: " + e);
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
			statement = con.prepareStatement("INSERT INTO ally_data (ally_id,ally_name,leader_id) values (?,?,?)");
			statement.setInt(1, getAllyId());
			statement.setString(2, getAllyName());
			statement.setInt(3, getLeaderId());
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("UPDATE clan_data SET ally_id=? WHERE clan_id=?");
			statement.setInt(1, getAllyId());
			statement.setInt(2, getLeaderId());
			statement.execute();

			if(Config.DEBUG)
				_log.warn("New ally saved in db: " + getAllyId());
		}
		catch(Exception e)
		{
			_log.warn("error while saving new ally to db " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void storeNewMemberInDatabase(L2Clan member)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET ally_id=? WHERE clan_id=?");
			statement.setInt(1, getAllyId());
			statement.setInt(2, member.getClanId());
			statement.execute();

			if(Config.DEBUG)
				_log.warn("New alliance member saved in db: " + getAllyId());
		}
		catch(Exception e)
		{
			_log.warn("error while saving new alliance member to db " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void removeMemberInDatabase(L2Clan member)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET ally_id=0 WHERE clan_id=?");
			statement.setInt(1, member.getClanId());
			statement.execute();

			if(Config.DEBUG)
				_log.warn("ally member removed in db: " + getAllyId());
		}
		catch(Exception e)
		{
			_log.warn("error while removing ally member in db " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void restore()
	{
		if(getAllyId() == 0) // no ally
			return;

		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		ResultSet rset = null, rset2 = null;
		try
		{
			L2Clan member;

			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT ally_name,leader_id FROM ally_data where ally_id=?");
			statement.setInt(1, getAllyId());
			rset = statement.executeQuery();

			if(rset.next())
			{
				setAllyName(rset.getString("ally_name"));
				int leaderId = rset.getInt("leader_id");

				statement2 = con.prepareStatement("SELECT clan_id,clan_name FROM clan_data WHERE ally_id=?");
				statement2.setInt(1, getAllyId());
				rset2 = statement2.executeQuery();

				while(rset2.next())
				{
					member = ClanTable.getInstance().getClan(rset2.getInt("clan_id"));
					if(member != null)
						if(member.getClanId() == leaderId)
							setLeader(member);
						else
							addAllyMember(member, false);
				}
			}

			setAllyCrestId(CrestCache.getAllyCrestId(getAllyId()));
		}
		catch(Exception e)
		{
			_log.warn("error while restoring ally");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(statement2, rset2);
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void broadcastToOnlineMembers(L2GameServerPacket packet)
	{
		for(L2Clan member : _members.values())
			if(member != null)
				member.broadcastToOnlineMembers(packet);
	}

	public void broadcastToOtherOnlineMembers(L2GameServerPacket packet, L2Player player)
	{
		for(L2Clan member : _members.values())
			if(member != null)
				member.broadcastToOtherOnlineMembers(packet, player);
	}

	@Override
	public String toString()
	{
		return getAllyName();
	}

	public boolean hasAllyCrest()
	{
		return _allyCrestId > 0;
	}

	public L2Player[] getOnlineMembers(String exclude)
	{
		List<L2Player> result = new FastList<L2Player>();
		for(L2Clan temp : _members.values())
			for(L2ClanMember temp2 : temp.getMembers())
				if(temp2.isOnline() && temp2.getPlayer() != null && (exclude == null || !temp2.getName().equals(exclude)))
					result.add(temp2.getPlayer());

		return result.toArray(new L2Player[result.size()]);
	}
}