package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressWarnings({"nls", "unqualified-field-access", "boxing"})
public class CharNameTable
{
	private static final Log _log = LogFactory.getLog(CharNameTable.class.getName());

	private static CharNameTable _instance;

	public static CharNameTable getInstance()
	{
		if(_instance == null)
			_instance = new CharNameTable();
		return _instance;
	}

	public boolean doesCharNameExist(String name)
	{
		if(FakePlayersTable.isFakePlayer(name))
			return true;

		boolean result = true;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT account_name FROM characters WHERE char_name=?");
			statement.setString(1, name);
			rset = statement.executeQuery();
			result = rset.next();
		}
		catch(SQLException e)
		{
			_log.warn("could not check existing charname:" + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		if(!result)
		{
			try
			{
				String query = "SELECT * FROM maintenance_task WHERE name='ChangeNicknameTask' AND result=1 AND param LIKE '%:" + name + ":%:%:%'";
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(query);
				rset = statement.executeQuery();
				result = rset.next();
			}
			catch(SQLException e)
			{
				_log.warn("could not check existing charname:" + e.getMessage());
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
		return result;
	}

	public int accountCharNumber(String account)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		int number = 0;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT COUNT(char_name) FROM characters WHERE account_name=?");
			statement.setString(1, account);
			rset = statement.executeQuery();
			while(rset.next())
				number = rset.getInt(1);
		}
		catch(SQLException e)
		{
			_log.warn("could not check existing char number:" + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return number;
	}
}