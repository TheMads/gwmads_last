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

/**
 * @author: rage
 * @date: 22.02.2010 12:13:48
 */
public class RewardTopRankingTask extends MaintenanceTask
{
	private static Log _log = LogFactory.getLog("maintenance");
	private String _lastResult = "";
	private static int[] _reward = { 0, 1250, 900, 700, 600, 450, 350, 300, 200, 150, 100,
									25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
									25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
									25, 25, 25, 25, 25, 25, 25, 25, 25, 25};
	@Override
	public boolean doTask(String params)
	{
		Connection con;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("SELECT cr.obj_id,c.clanid,c.char_name,sum(cr.points) points FROM character_rbp cr INNER JOIN characters c on (c.obj_id=cr.obj_id) GROUP BY 1 ORDER BY 4 DESC");
			rs = stmt.executeQuery();
			int rank = 1;
			int lastPoints = 0;
			while(rs.next())
			{
				if(rank >= _reward.length)
					break;

				int clanId = rs.getInt("clanid");
				if(clanId > 0)
				{
					L2Clan clan = ClanTable.getInstance().getClan(clanId);
					if(clan != null && clan.getLevel() > 4)
					{
						_log.info("RewardTopRankingTask: reward " + _reward[rank] + " CRP to clan: " + clan.getName() + " for player: " + rs.getString("char_name") + " rank: " + rank);
						clan.incReputation(_reward[rank], false, "RewardTopRank");
					}
				}
				else
					_log.info("RewardTopRankingTask: skip reward " + _reward[rank] + " CRP no clan, for player: " + rs.getString("char_name") + " rank: " + rank);

				if(lastPoints != rs.getInt("points"))
				{
					lastPoints = rs.getInt("points");
					rank++;
				}
			}
			stmt.close();
			stmt = con.prepareStatement("DELETE FROM character_rbp");
			stmt.execute();
		}
		catch(Exception e)
		{
			_lastResult = "SQL Error";
			_log.warn("RewardTopRankingTask: can't execute sql: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(stmt, rs);
			addTask("");
		}

		_lastResult = "Top Ranking Rewarded";
		return true;
	}

	@Override
	public void addTask(String params)
	{
		Connection con = null;
		PreparedStatement stmt = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("REPLACE INTO maintenance_task(`name`, `param`, `status`, `result`, `datetime`) VALUES('RewardTopRankingTask', ?, 0, 0, ?)");
			stmt.setString(1, String.valueOf(MaintenanceManager.getInstance().getMaintenanceTime()));
			stmt.setInt(2, MaintenanceManager.getInstance().getMaintenanceTime());
			stmt.execute();
		}
		catch(Exception e)
		{
			_log.warn("RewardTopRankingTask: can't update sql");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
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
