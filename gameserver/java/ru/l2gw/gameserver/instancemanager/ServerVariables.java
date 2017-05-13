package ru.l2gw.gameserver.instancemanager;

import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.templates.StatsSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ServerVariables
{
	private static StatsSet server_vars = null;

	private static StatsSet getVars()
	{
		if(server_vars == null)
		{
			server_vars = new StatsSet();
			LoadFromDB();
		}
		return server_vars;
	}

	private static void LoadFromDB()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM server_variables");
			rs = statement.executeQuery();
			while(rs.next())
				server_vars.set(rs.getString("name"), rs.getString("value"));
		}
		catch(Exception e)
		{}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	private static void SaveToDB(String name)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			String value = server_vars.getString(name, "");
			if(value.isEmpty())
			{
				statement = con.prepareStatement("DELETE FROM server_variables WHERE name = ?");
				statement.setString(1, name);
				statement.execute();
			}
			else
			{
				statement = con.prepareStatement("REPLACE INTO server_variables (name, value) VALUES (?,?)");
				statement.setString(1, name);
				statement.setString(2, value);
				statement.execute();
			}
		}
		catch(Exception e)
		{}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public static boolean getBool(String name)
	{
		return getVars().getBool(name);
	}

	public static boolean getBool(String name, boolean _defult)
	{
		return getVars().getBool(name, _defult);
	}

	public static int getInt(String name)
	{
		return getVars().getInteger(name);
	}

	public static int getInt(String name, int _defult)
	{
		return getVars().getInteger(name, _defult);
	}

	public static long getLong(String name)
	{
		return getVars().getLong(name);
	}

	public static long getLong(String name, long _defult)
	{
		return getVars().getLong(name, _defult);
	}

	public static float getFloat(String name)
	{
		return getVars().getFloat(name);
	}

	public static float getFloat(String name, float _defult)
	{
		return getVars().getFloat(name, _defult);
	}

	public static String getString(String name)
	{
		return getVars().getString(name);
	}

	public static String getString(String name, String _defult)
	{
		return getVars().getString(name, _defult);
	}

	public static void set(String name, boolean value)
	{
		getVars().set(name, value);
		SaveToDB(name);
	}

	public static void set(String name, int value)
	{
		getVars().set(name, value);
		SaveToDB(name);
	}

	public static void set(String name, long value)
	{
		getVars().set(name, value);
		SaveToDB(name);
	}

	public static void set(String name, double value)
	{
		getVars().set(name, value);
		SaveToDB(name);
	}

	public static void set(String name, String value)
	{
		getVars().set(name, value);
		SaveToDB(name);
	}

	public static void unset(String name)
	{
		getVars().unset(name);
		SaveToDB(name);
	}
}