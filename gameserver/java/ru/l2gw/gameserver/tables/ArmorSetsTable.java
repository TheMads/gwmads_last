package ru.l2gw.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2ArmorSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ArmorSetsTable
{
	private static Log _log = LogFactory.getLog(ArmorSetsTable.class.getName());
	private static ArmorSetsTable _instance;
	private boolean _initialized = true;

	private TIntObjectHashMap<L2ArmorSet> _armorSets;

	public static ArmorSetsTable getInstance()
	{
		if(_instance == null)
			_instance = new ArmorSetsTable();
		return _instance;
	}

	private ArmorSetsTable()
	{
		_armorSets = new TIntObjectHashMap<>();
		loadData();
	}

	private void loadData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT chest, legs, head, gloves, feet, skill_id, shield, shield_skill_id, enchant6skill FROM armorsets");
			rset = statement.executeQuery();

			while(rset.next())
				_armorSets.put(rset.getInt("chest"), new L2ArmorSet(rset.getInt("chest"), rset.getString("legs"), rset.getString("head"), rset.getString("gloves"), rset.getString("feet"), rset.getString("shield"), rset.getString("skill_id"), rset.getString("shield_skill_id"), rset.getString("enchant6skill")));

			_log.info("ArmorSetsTable: Loaded " + _armorSets.size() + " armor sets.");
		}
		catch(Exception e)
		{
			_log.warn("ArmorSetsTable: Error reading ArmorSets table: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public L2ArmorSet getSet(int chestId)
	{
		return _armorSets.get(chestId);
	}

	public boolean isInitialized()
	{
		return _initialized;
	}
}