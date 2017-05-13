package ru.l2gw.accountmanager;

import ru.l2gw.commons.utils.Base64;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.loginserver.Config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLAccountManager
{
	private static String _uname = "";
	private static String _pass = "";
	private static String _level = "";
	private static String _comments = "";
	private static String _mode = "";

	public static void main(String[] args) throws SQLException, IOException, NoSuchAlgorithmException
	{
		Config.load();
		System.out.println("Please choose an option:");
		System.out.println("");
		System.out.println("1 - Create new account or update existing one (change pass and access level).");
		System.out.println("2 - Change access level.");
		System.out.println("3 - Delete existing account.");
		System.out.println("4 - List accounts & access levels.");
		System.out.println("5 - Exit.");
		LineNumberReader _in = new LineNumberReader(new InputStreamReader(System.in));
		while(!(_mode.equals("1") || _mode.equals("2") || _mode.equals("3") || _mode.equals("4") || _mode.equals("5")))
		{
			System.out.print("Your choice: ");
			_mode = _in.readLine();
		}

		if(_mode.equals("1") || _mode.equals("2") || _mode.equals("3"))
		{
			if(_mode.equals("1") || _mode.equals("2") || _mode.equals("3"))
				while(_uname.length() == 0)
				{
					System.out.print("username: ");
					_uname = _in.readLine();
				}

			if(_mode.equals("1"))
				while(_pass.length() == 0)
				{
					System.out.print("password: ");
					_pass = _in.readLine();
				}

			if(_mode.equals("1") || _mode.equals("2"))
				while(_level.length() == 0)
				{
					System.out.print("access level: ");
					_level = _in.readLine();
				}

			if(_mode.equals("1") || _mode.equals("2"))
				while(_comments.length() == 0)
				{
					System.out.print("comments: ");
					_comments = _in.readLine();
				}

		}

		if(_mode.equals("1"))
			// Add or Update
			AddOrUpdateAccount(_uname, _pass, _level, _comments);
		else if(_mode.equals("2"))
			// Change Level
			ChangeAccountLevel(_uname, _level);
		else if(_mode.equals("3"))
		{
			// Delete
			System.out.print("Do you really want to delete this account ? Y/N : ");
			String yesno = _in.readLine();
			if(yesno.equals("Y"))
				// Yes
				DeleteAccount(_uname);

		}
		else if(_mode.equals("4"))
			// List
			printAccInfo();

		System.out.println("Have fun playing lineage2");
	}

	private static void printAccInfo() throws SQLException
	{
		int count = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT login, access_level FROM accounts ORDER BY login ASC");
			rset = statement.executeQuery();
			while(rset.next())
			{
				System.out.println(rset.getString("login") + " -> " + rset.getInt("access_level"));
				count++;
			}
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		System.out.println("Number of accounts: " + count);
	}

	private static void AddOrUpdateAccount(String account, String password, String level, String comments) throws IOException, SQLException, NoSuchAlgorithmException
	{
		// Encode Password
		MessageDigest md = MessageDigest.getInstance("SHA");
		byte[] newpass;
		newpass = password.getBytes("UTF-8");
		newpass = md.digest(newpass);
		if(Integer.parseInt(level) > 100)
			level = "100";

		// Add to Base
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE	accounts (login, password, access_level, comments) VALUES (?,?,?,?)");
			statement.setString(1, account);
			statement.setString(2, Base64.encodeBytes(newpass));
			statement.setString(3, level);
			statement.setString(4, comments);
			statement.executeUpdate();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private static void ChangeAccountLevel(String account, String level) throws SQLException
	{
		if(Integer.parseInt(level) > 100)
			level = "100";
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			// Check Account Exist
			statement = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login=?;");
			statement.setString(1, account);
			rset = statement.executeQuery();
			if(rset.next() && rset.getInt(1) > 0)
			{
				// Exist

				// Update
				statement = con.prepareStatement("UPDATE accounts SET access_level=? WHERE login=?;");
				statement.setEscapeProcessing(true);
				statement.setString(1, level);
				statement.setString(2, account);
				statement.executeUpdate();

				System.out.println("Account \"" + account + "\" Updated\n");
			}
			else
				// Not Exist
				System.out.println("Account \"" + account + "\" Not Exist\n");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private static void DeleteAccount(String account) throws SQLException
	{
		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement subStatement = null;
		ResultSet rset = null, subRset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			// Check Account Exist
			statement = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login=?;");
			statement.setString(1, account);
			rset = statement.executeQuery();
			if(rset.getInt(1) == 0)
			{
				System.out.println("Account \"" + account + "\" Not Exist\n");
				return;
			}
			// Account exist
			DbUtils.closeQuietly(statement, rset);

			// Get Accounts ID
			statement = con.prepareStatement("SELECT obj_Id, char_name, clanid FROM characters WHERE account_name=?;");

			statement.setEscapeProcessing(true);
			statement.setString(1, account);
			rset = statement.executeQuery();
			while(rset.next())
			{
				System.out.println("Deleting character \"" + rset.getString("char_name") + "\"\n");
				// Check If clan leader Remove Clan and remove all from it
				subStatement = con.prepareStatement("SELECT COUNT(*) FROM clan_data WHERE leader_id=?;");
				subStatement.setString(1, rset.getString("clanid"));
				subRset = subStatement.executeQuery();
				boolean isClanLeader = false;
				if(subRset.next() && subRset.getInt(1) > 0)
					isClanLeader = true;

				DbUtils.closeQuietly(subStatement, subRset);
				if(isClanLeader)
				{
					// Clan Leader

					// Get Clan Name
					subStatement = con.prepareStatement("SELECT clan_name FROM clan_data WHERE leader_id=?;");
					subStatement.setString(1, rset.getString("clanid"));
					subRset = subStatement.executeQuery();

					String clanName = null;
					if(subRset.next())
						clanName = subRset.getString("clan_name");

					System.out.println("Deleting clan \"" + clanName + "\"\n");

					DbUtils.closeQuietly(subStatement, subRset);

					// Delete Clan Wars
					subStatement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?;");
					subStatement.setEscapeProcessing(true);
					subStatement.setString(1, clanName);
					subStatement.setString(2, clanName);
					subStatement.executeUpdate();
					DbUtils.closeQuietly(subStatement);

					// Remove All From clan
					subStatement = con.prepareStatement("UPDATE characters SET clanid=0 WHERE clanid=?;");
					subStatement.setString(1, rset.getString("clanid"));
					subStatement.executeUpdate();
					DbUtils.closeQuietly(subStatement);

					// Delete Clan
					subStatement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?;");
					subStatement.setString(1, rset.getString("clanid"));
					subStatement.executeUpdate();
					DbUtils.closeQuietly(subStatement);
				}

				// skills
				subStatement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=?;");
				subStatement.setString(1, rset.getString("obj_Id"));
				subStatement.executeUpdate();
				DbUtils.closeQuietly(subStatement);

				// shortcuts
				subStatement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?;");
				subStatement.setString(1, rset.getString("obj_Id"));
				subStatement.executeUpdate();
				DbUtils.closeQuietly(subStatement);

				// items
				subStatement = con.prepareStatement("DELETE FROM items WHERE owner_id=?;");
				subStatement.setString(1, rset.getString("obj_Id"));
				subStatement.executeUpdate();
				DbUtils.closeQuietly(subStatement);

				// recipebook
				subStatement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?;");
				subStatement.setString(1, rset.getString("obj_Id"));
				subStatement.executeUpdate();
				DbUtils.closeQuietly(subStatement);

				// quests
				subStatement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=?;");
				subStatement.setString(1, rset.getString("obj_Id"));
				subStatement.executeUpdate();
				DbUtils.closeQuietly(subStatement);

				// macroses
				subStatement = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id=?;");
				subStatement.setString(1, rset.getString("obj_Id"));
				subStatement.executeUpdate();
				DbUtils.closeQuietly(subStatement);

				// friends
				subStatement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? or friend_id = ?;");
				subStatement.setString(1, rset.getString("obj_Id"));
				subStatement.setString(2, rset.getString("obj_Id"));
				subStatement.executeUpdate();
				DbUtils.closeQuietly(subStatement);

				// boxaccess
				subStatement = con.prepareStatement("DELETE FROM boxaccess WHERE charname=?;");
				subStatement.setString(1, rset.getString("char_name"));
				subStatement.executeUpdate();
				DbUtils.closeQuietly(subStatement);

				// characters
				subStatement = con.prepareStatement("DELETE FROM characters WHERE obj_Id=?;");
				subStatement.setString(1, rset.getString("obj_Id"));
				subStatement.executeUpdate();
				DbUtils.closeQuietly(subStatement);
			}
			DbUtils.closeQuietly(statement, rset);

			// Delete Account
			statement = con.prepareStatement("DELETE FROM accounts WHERE login=?;");
			statement.setEscapeProcessing(true);
			statement.setString(1, account);
			statement.executeUpdate();
			System.out.println("Account \"" + account + "\" Deleted\n\n");

		}
		finally
		{
			DbUtils.closeQuietly(subStatement, subRset);
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

}
