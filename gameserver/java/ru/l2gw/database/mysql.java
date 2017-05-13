package ru.l2gw.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public abstract class mysql
{
	private static Log _log = LogFactory.getLog(mysql.class.getName());

	/**
	 * Выполняет простой sql запросов, где ненужен контроль параметров<BR>
	 * ВНИМАНИЕ: В данном методе передаваемые параметры не проходят проверку на предмет SQL-инъекции!
	 * @param query Строка SQL запроса
	 * @return false в случае ошибки выполнения запроса либо true в случае успеха
	 */
	public static boolean set(String query)
	{
		Connection con = null;
		Statement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			statement.executeUpdate(query);
		}
		catch(Exception e)
		{
			_log.warn("Could not execute update '" + query + "': " + e);
			Thread.dumpStack();
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}

	public static Object get(String query)
	{
		Object ret = null;
		Connection con = null;
		Statement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery(query + " LIMIT 1");
			ResultSetMetaData md = rset.getMetaData();

			if(rset.next())
				if(md.getColumnCount() > 1)
				{
					ConcurrentHashMap<String, Object> tmp = new ConcurrentHashMap<String, Object>();
					for(int i = 0; i < md.getColumnCount(); i++)
						tmp.put(md.getColumnName(i + 1), rset.getObject(i + 1));
					ret = tmp;
				}
				else
					ret = rset.getObject(1);

		}
		catch(Exception e)
		{
			_log.warn("Could not execute query '" + query + "': " + e);
			Thread.dumpStack();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return ret;
	}

	public static ArrayList<Object> get_array(String query)
	{
		ArrayList<Object> ret = new ArrayList<Object>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			rset = statement.executeQuery();
			ResultSetMetaData md = rset.getMetaData();

			while(rset.next())
				if(md.getColumnCount() > 1)
				{
					ConcurrentHashMap<String, Object> tmp = new ConcurrentHashMap<String, Object>();
					for(int i = 0; i < md.getColumnCount(); i++)
						tmp.put(md.getColumnName(i + 1), rset.getObject(i + 1));
					ret.add(tmp);
				}
				else
					ret.add(rset.getObject(1));
		}
		catch(Exception e)
		{
			_log.warn("Could not execute query '" + query + "': " + e);
			Thread.dumpStack();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return ret;
	}

	public static int simple_get_int(String ret_field, String table, String where)
	{
		String query = "SELECT " + ret_field + " FROM `" + table + "` WHERE " + where + " LIMIT 1;";

		int res = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			rset = statement.executeQuery();

			if(rset.next())
				res = rset.getInt(1);
		}
		catch(Exception e)
		{
			_log.warn("mSGI: Error in query '" + query + "':" + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return res;
	}

	public static long simple_get_long(String ret_field, String table, String where)
	{
		String query = "SELECT " + ret_field + " FROM `" + table + "` WHERE " + where + " LIMIT 1;";

		long res = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			rset = statement.executeQuery();

			if(rset.next())
				res = rset.getLong(1);
		}
		catch(Exception e)
		{
			_log.warn("mSGI: Error in query '" + query + "':" + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return res;
	}

	public static Integer[][] simple_get_int_array(String[] ret_fields, String table, String where)
	{
		long start = System.currentTimeMillis();

		String fields = null;
		for(String field : ret_fields)
			if(fields != null)
			{
				fields += ",";
				fields += "`" + field + "`";
			}
			else
				fields = "`" + field + "`";

		String query = "SELECT " + fields + " FROM `" + table + "` WHERE " + where;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		Integer res[][] = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			rset = statement.executeQuery();

			ArrayList<Integer[]> al = new ArrayList<Integer[]>();
			int row = 0;
			while(rset.next())
			{
				Integer[] tmp = new Integer[ret_fields.length];
				for(int i = 0; i < ret_fields.length; i++)
					tmp[i] = rset.getInt(i + 1);
				al.add(row, tmp);
				row++;
			}

			res = al.toArray(new Integer[row][ret_fields.length]);
		}
		catch(Exception e)
		{
			_log.warn("mSGIA: Error in query '" + query + "':" + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		_log.debug("Get all rows in query '" + query + "' in " + (System.currentTimeMillis() - start) + "ms");
		return res;
	}
}
