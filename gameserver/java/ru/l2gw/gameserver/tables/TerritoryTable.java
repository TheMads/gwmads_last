package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Territory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

public class TerritoryTable
{
	private static Log _log = LogFactory.getLog(TerritoryTable.class.getName());
	private static final TerritoryTable _instance = new TerritoryTable();
	private static HashMap<String, L2Territory> _locations;

	public static TerritoryTable getInstance()
	{
		return _instance;
	}

	private TerritoryTable()
	{
		reloadData();
	}

	public L2Territory getLocation(int terr)
	{
		String name = "sql_terr_" + terr;
		L2Territory t = _locations.get(name);
		if(t == null)
			_log.warn("Error territory[49] " + name);
		return t;
	}

	public int[] getRandomPoint(int terr, boolean air)
	{
		String name = "sql_terr_" + terr;
		L2Territory t = _locations.get(name);
		if(t == null)
		{
			_log.warn("Error territory[54] " + name);
			return new int[3];
		}
		return t.getRandomPoint(air);
	}

	public int getMinZ(int terr)
	{
		String name = "sql_terr_" + terr;
		L2Territory t = _locations.get(name);
		if(t == null)
		{
			_log.warn("Error territory[61] " + name);
			return 0;
		}
		return t.getZmin();
	}

	public int getMaxZ(int terr)
	{
		String name = "sql_terr_" + terr;
		L2Territory t = _locations.get(name);
		if(t == null)
		{
			_log.warn("Error territory[73] " + name);
			return 0;
		}
		return t.getZmax();
	}

	public void reloadData()
	{
		_locations = new HashMap<String, L2Territory>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT loc_id, loc_x, loc_y, loc_zmin, loc_zmax FROM `locations` ORDER BY ordr");
			rset = statement.executeQuery();
			while(rset.next())
			{
				String terr = "sql_terr_" + rset.getString("loc_id");

				if(_locations.get(terr) == null)
				{
					L2Territory t = new L2Territory(terr);
					_locations.put(terr, t);
				}
				_locations.get(terr).add(rset.getInt("loc_x"), rset.getInt("loc_y"), rset.getInt("loc_zmin"), rset.getInt("loc_zmax"));
			}

			DbUtils.closeQuietly(statement, rset);

			statement = con.prepareStatement("SELECT loc_id, banned_loc_id FROM `banned_locations`");
			rset = statement.executeQuery();
			while(rset.next())
			{
				L2Territory terr = _locations.get("sql_terr_" + rset.getString("loc_id"));
				if(terr != null)
					terr.addBannedTerritory(_locations.get("sql_terr_" + rset.getString("banned_loc_id")));
			}
		}
		catch(Exception e1)
		{
			//problem with initializing spawn, go to next one
			_log.warn("locations couldnt be initialized:" + e1);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		_log.info("TerritoryTable: Loaded " + _locations.size() + " locations");
	}

	public HashMap<String, L2Territory> getLocations()
	{
		return _locations;
	}
}