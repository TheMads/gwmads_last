package ru.l2gw.extensions.maintenance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.instancemanager.MaintenanceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.tables.ClanTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;

/**
 * @author rage
 * @date 13.08.2009 10:00:53
 */
public class ChangeClanLeaderTask extends MaintenanceTask
{
	private static Log _log = LogFactory.getLog("maintenance");
	private String _lastResult = "";

	/*
	 * params format:
	 * clanId:newClanLeaderId
	 */
	@Override
	public boolean doTask(String params)
	{
		StringTokenizer st = new StringTokenizer(params, ":");
		int clanId;
		int leaderId;

		Connection con;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try
		{
			clanId = Integer.parseInt(st.nextToken());
			leaderId = Integer.parseInt(st.nextToken());
		}
		catch(NumberFormatException e)
		{
			_log.warn("ChangeClanLeaderTask: can't parse params: " + params);
			e.printStackTrace();
			return false;
		}

		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("SELECT clanid,pledge_type FROM characters WHERE obj_id = ?");
			stmt.setInt(1, leaderId);
			rs = stmt.executeQuery();
			if(rs.next())
			{
				int dbClanId = rs.getInt(1);
				int pledgeType = rs.getInt(2);
				if(pledgeType != 0)
				{
					_log.warn("ChangeClanLeaderTask: can't change clan leader to: " + leaderId + " for clan_id: " + clanId + " leader not main in clan");
					_lastResult = "leader not in main clan";
					return false;
				}

				if(dbClanId == clanId)
				{
					L2Clan clan = ClanTable.getInstance().getClan(clanId);
					clan.updateClanleader(leaderId, con);
				}
				else
				{
					_log.warn("ChangeClanLeaderTask: can't change clan leader to: " + leaderId + " for clan_id: " + clanId + " leader not in clan");
					_lastResult = "leader not in clan";
					return false;
				}
			}
			else
			{
				_log.warn("ChangeClanLeaderTask: char not found: " + leaderId);
				_lastResult = "char not found";
				return false;
			}
		}
		catch(Exception e)
		{
			_log.warn("ChangeClanLeaderTask: can't update sql:");
			e.printStackTrace();
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(stmt, rs);
		}

		return true;
	}

	@Override
	public void addTask(String params)
	{
		StringTokenizer st = new StringTokenizer(params, ":");
		int clanId;
		int leaderId;

		Connection con = null;
		PreparedStatement stmt;
		PreparedStatement stmt2;
		ResultSet rs;

		try
		{
			clanId = Integer.parseInt(st.nextToken());
			leaderId = Integer.parseInt(st.nextToken());
		}
		catch(NumberFormatException e)
		{
			_log.warn("ChangeClanLeaderTask: can't parse params: " + params);
			e.printStackTrace();
			return;
		}

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("SELECT * FROM maintenance_task WHERE status = 0 and param like '" + clanId + ":%'");
			rs = stmt.executeQuery();
			if(rs.next())
			{
				int id = rs.getInt(1);
				stmt2 = con.prepareStatement("DELETE FROM maintenance_task WHERE id = ?");
				stmt2.setInt(1, id);
				stmt2.execute();
				stmt2.close();
			}
			stmt.close();

			stmt = con.prepareStatement("REPLACE INTO maintenance_task(`name`, `param`, `status`, `result`, `datetime`) VALUES('ChangeClanLeaderTask', ?, 0, 0, ?)");
			stmt.setString(1, clanId + ":" + leaderId);
			stmt.setInt(2, MaintenanceManager.getInstance().getMaintenanceTime());
			stmt.execute();
			stmt.close();
		}
		catch(Exception e)
		{
			_log.warn("ChangeClanLeaderTask: can't update sql:");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}

	@Override
	public String getLastResult()
	{
		String ret = _lastResult;
		_lastResult = "";
		return ret;
	}
}
