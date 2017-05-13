package ru.l2gw.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.templates.L2Henna;
import ru.l2gw.gameserver.templates.StatsSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class HennaTable
{
	private static final Log _log = LogFactory.getLog(HennaTable.class.getName());

	private static HennaTable _instance;

	private TIntObjectHashMap<L2Henna> _henna;
	private boolean _initialized = true;

	public static HennaTable getInstance()
	{
		if(_instance == null)
			_instance = new HennaTable();
		return _instance;
	}

	private HennaTable()
	{
		_henna = new TIntObjectHashMap<>();
		RestoreHennaData();

	}

	private void RestoreHennaData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet hennadata = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT symbol_id, symbol_name, dye_id, dye_amount, price, stat_INT, stat_STR, stat_CON, stat_MEM, stat_DEX, stat_WIT FROM henna");
			hennadata = statement.executeQuery();

			fillHennaTable(hennadata);
		}
		catch(Exception e)
		{
			_log.warn("error while creating henna table " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, hennadata);
		}
	}

	private void fillHennaTable(ResultSet HennaData) throws Exception
	{
		while(HennaData.next())
		{
			StatsSet hennaDat = new StatsSet();
			int id = HennaData.getInt("symbol_id");

			hennaDat.set("symbol_id", id);
			//hennaDat.set("symbol_name", HennaData.getString("symbol_name"));
			hennaDat.set("dye", HennaData.getInt("dye_id"));
			hennaDat.set("price", HennaData.getInt("price"));
			//amount of dye required
			hennaDat.set("amount", HennaData.getInt("dye_amount"));
			hennaDat.set("stat_INT", HennaData.getInt("stat_INT"));
			hennaDat.set("stat_STR", HennaData.getInt("stat_STR"));
			hennaDat.set("stat_CON", HennaData.getInt("stat_CON"));
			hennaDat.set("stat_MEM", HennaData.getInt("stat_MEM"));
			hennaDat.set("stat_DEX", HennaData.getInt("stat_DEX"));
			hennaDat.set("stat_WIT", HennaData.getInt("stat_WIT"));

			L2Henna template = new L2Henna(hennaDat);
			_henna.put(id, template);
		}
		_log.info("HennaTable: Loaded " + _henna.size() + " Templates.");
	}

	public boolean isInitialized()
	{
		return _initialized;
	}

	public L2Henna getTemplate(int id)
	{
		return _henna.get(id);
	}

}
