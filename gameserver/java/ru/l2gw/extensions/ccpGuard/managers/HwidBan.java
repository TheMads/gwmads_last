package ru.l2gw.extensions.ccpGuard.managers;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.ccpGuard.ProtectInfo;
import ru.l2gw.gameserver.network.GameClient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HwidBan
{
	private static HwidBan _instance;
	private static GArray<L2HwidBan> _lists;

	public static HwidBan getInstance()
	{
		if(_instance == null)
			_instance = new HwidBan();
		return _instance;
	}

	public static void reload()
	{
		_instance = new HwidBan();
	}

	public HwidBan()
	{
		_lists = new GArray<L2HwidBan>();
		load();
	}

	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM hwid_bans");
			rset = statement.executeQuery();
			while(rset.next())
				_lists.add(new L2HwidBan(rset.getString("HWID")));
		}
		catch(Exception E)
		{
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public static boolean checkFullHWIDBanned(ProtectInfo pi)
	{
		if(_lists.size() > 0)
			for(L2HwidBan hw : _lists)
				if(hw.getHwid().equals(pi.getHWID()))
					return true;

		return false;
	}

	public static int getCountHwidBan()
	{
		return _lists.size();
	}

	public static void addHwidBan(GameClient client, String comment)
	{
		addHwidBan(client._prot_info.getHWID(), comment);
	}

	public static void addHwidBan(String hwid, String comment)
	{
		_lists.add(new L2HwidBan(hwid));
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO hwid_bans VALUES(?, '', -1, ?)");
			statement.setString(1, hwid);
			statement.setString(2, comment);
			statement.execute();
			DbUtils.closeQuietly(statement);
		}
		catch(Exception e)
		{
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

}