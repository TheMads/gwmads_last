package ru.l2gw.gameserver.model.entity.siege.ClanHall;

import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeDatabase;
import ru.l2gw.gameserver.tables.ClanTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *@author FlareDrakon
 */
public class ClanHallSiegeDatabase extends SiegeDatabase
{

	public ClanHallSiegeDatabase(Siege siege)
	{
		super(siege);
	}

	@Override
	public void saveSiegeDate()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE ch_sieges SET starttime=? where ch_id=?");
			statement.setLong(1, _siege.getSiegeDate().getTimeInMillis() / 1000);
			statement.setInt(2, _siege.getSiegeUnit().getId());
			statement.executeUpdate();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

	}

	@Override
	public void saveSettings(int clanId, int memberId, int npcValue)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO `ChSiegeSettings` (`clanId`,`unitId`,`memberId`,`npcindex` values (?,?,?,?)");
			statement.setInt(1, clanId);
			statement.setInt(2, _siege.getSiegeUnit().getId());
			statement.setInt(3, memberId);
			statement.setInt(3, npcValue);
			statement.execute();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public static void clearSiegeClan(ClanHallSiege siege)
	{
		SiegeDatabase.clearSiegeClan(siege);
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE from `ChSiegeSettings` WHERE `unitId`=?");
			statement.setInt(1, siege.getSiegeUnit().getId());
			statement.execute();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		siege.getAttakerClans().clear();
	}

	public static void loadSiegeClan(ClanHallSiege siege)
	{
		SiegeDatabase.loadSiegeClan(siege);
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		siege.getAttakerClans().clear();
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * from `ChSiegeSettings` WHERE `unitId`=?");
			statement.setInt(1, siege.getSiegeUnit().getId());
			rset = statement.executeQuery();
			while(rset.next())
			{
				L2Clan clan = ClanTable.getInstance().getClan(rset.getInt("clanId"));
				L2Player pl = clan.getClanMember(rset.getInt("memberId")).getPlayer();
				siege.addMember(pl, false);
				siege.setNpc(rset.getInt("clanId"), rset.getInt("npcindex"), false);
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public static void removeSiegeClan(int clanId,ClanHallSiege siege)
	{
		Connection con = null;
		PreparedStatement statement = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE from `ChSiegeSettings` WHERE `unitId`=? and clanId=?");
			statement.setInt(1, siege.getSiegeUnit().getId());
			statement.setInt(2, clanId);
			statement.execute();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		SiegeDatabase.removeSiegeClan(clanId, siege);
	}

	@Override
	public void saveLastSiegeDate()
	{}

}
