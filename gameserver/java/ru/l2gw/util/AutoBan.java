package ru.l2gw.util;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class AutoBan
{
	private static org.apache.commons.logging.Log _log = LogFactory.getLog(AutoBan.class.getName());

	public static boolean isBanned(int ObjectId)
	{
		boolean res = false;
		Integer acl = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT MAX(endban) endban,accesslevel acl FROM characters c left outer join bans b on c.obj_id=b.obj_id where c.obj_id=? group by c.obj_id");
			statement.setInt(1, ObjectId);
			rset = statement.executeQuery();

			while(rset.next())
			{
				Long endban = rset.getLong("endban") * 1000;
				res = endban > System.currentTimeMillis();
				acl = rset.getInt("acl");
			}
		}
		catch(Exception e)
		{
			_log.warn("Could not restore ban data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return (res) || (acl == -350);
	}

	public static int GetIDbyName(String name)
	{
		int res = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT obj_Id FROM characters WHERE char_name=?");
			statement.setString(1, name);
			rset = statement.executeQuery();

			while(rset.next())
				res = rset.getInt("obj_Id");
		}
		catch(Exception e)
		{
			_log.warn("Could not find char: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return res;
	}

	public static void Banned(L2Player actor, int period, String msg, String GM)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			Long srok = System.currentTimeMillis() + period * 60000;
			String date = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date());
			String enddate = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date(srok));
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO bans (account_name, obj_id, baned, unban, reason, GM, endban) VALUES(?,?,?,?,?,?,?)");
			statement.setString(1, actor.getAccountName());
			statement.setInt(2, actor.getObjectId());
			statement.setString(3, date);
			statement.setString(4, enddate);
			statement.setString(5, msg);
			statement.setString(6, GM);
			statement.setLong(7, srok / 1000);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("could not store bans data:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	//offline
	public static boolean Banned(String actor, int acc_level, int period, String msg, String GM)
	{
		boolean res;
		int obj_id = GetIDbyName(actor);
		res = obj_id > 0;
		Connection con = null;
		PreparedStatement statement = null;
		if(res)
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE characters SET accesslevel=? WHERE obj_Id=?");
				statement.setInt(1, acc_level);
				statement.setInt(2, obj_id);
				statement.execute();
				DbUtils.closeQuietly(statement);
				if(acc_level < 0)
				{
					Long srok = System.currentTimeMillis() + period * 24 * 3600 * 1000;
					String date = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date());
					String enddate = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date(srok));

					statement = con.prepareStatement("INSERT INTO bans (obj_id, baned, unban, reason, GM, endban) VALUES(?,?,?,?,?,?)");
					statement.setInt(1, obj_id);
					statement.setString(2, date);
					statement.setString(3, enddate);
					statement.setString(4, msg);
					statement.setString(5, GM);
					statement.setLong(6, srok / 1000);
					statement.execute();
				}
				else
				{
					statement = con.prepareStatement("DELETE FROM bans WHERE obj_id=?");
					statement.setInt(1, obj_id);
					statement.execute();
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				_log.warn("could not store bans data:" + e);
				res = false;
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		return res;
	}

	public static void Karma(L2Player actor, int karma, String msg, String GM)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			String date = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date());
			msg = "Add karma(" + karma + ") " + msg;
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO bans (account_name, obj_id, baned, reason, GM) VALUES(?,?,?,?,?)");
			statement.setString(1, actor.getAccountName());
			statement.setInt(2, actor.getObjectId());
			statement.setString(3, date);
			statement.setString(4, msg);
			statement.setString(5, GM);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("could not store bans data:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	//offline
	public static boolean Karma(String actor, int karma, String msg, String GM)
	{
		boolean res;
		int obj_id = GetIDbyName(actor);
		res = obj_id > 0;
		Connection con = null;
		PreparedStatement statement = null;

		if(res)
			try
			{
				con = DatabaseFactory.getInstance().getConnection();

				statement = con.prepareStatement("update characters set karma=karma + ? where obj_Id=?");
				statement.setInt(1, karma);
				statement.setInt(2, obj_id);
				statement.execute();
				DbUtils.closeQuietly(statement);

				String date = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date());
				msg = "Add karma(" + karma + ") " + msg;
				statement = con.prepareStatement("INSERT INTO bans (obj_id, baned, reason, GM) VALUES(?,?,?,?)");
				statement.setInt(1, obj_id);
				statement.setString(2, date);
				statement.setString(3, msg);
				statement.setString(4, GM);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.warn("could not store bans data:" + e);
				res = false;
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		return res;
	}

	public static void Banned(L2Player actor, int period, String msg)
	{
		Banned(actor, period, msg, "AutoBan");
	}

	public static boolean ChatBan(String actor, int period, String msg, String GM)
	{
		boolean res = true;
		long NoChannel = period * 60000;
		L2Player plyr = L2ObjectsStorage.getPlayer(actor);

		Connection con = null;
		PreparedStatement statement = null;
		if(plyr != null)
		{

			plyr.sendPacket(new SystemMessage(SystemMessage.CHATTING_IS_PROHIBITED));
			plyr.updateNoChannel(NoChannel);
		}
		else
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE characters SET nochannel = ? WHERE obj_Id=?");
				statement.setLong(1, NoChannel > 0 ? NoChannel / 1000 : NoChannel);
				statement.setInt(2, GetIDbyName(actor));
				statement.executeUpdate();
			}
			catch(Exception e)
			{
				res = false;
				_log.warn("Could not activate nochannel:" + e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}

		if(res)
			try
			{
				con = DatabaseFactory.getInstance().getConnection();

				String date = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date(System.currentTimeMillis()));
				String enddate = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date(System.currentTimeMillis() + NoChannel));

				statement = con.prepareStatement("INSERT INTO bans (obj_id, baned, unban, reason, GM) VALUES(?,?,?,?,?)");
				statement.setInt(1, GetIDbyName(actor));
				statement.setString(2, date);
				statement.setString(3, enddate);
				statement.setString(4, msg);
				statement.setString(5, GM);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.warn("could not store bans data:" + e);
				res = false;
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		return res;
	}

	public static boolean ChatUnBan(String actor, String GM)
	{
		boolean res = true;
		L2Player plyr = L2ObjectsStorage.getPlayer(actor);

		Connection con = null;
		PreparedStatement statement = null;
		if(plyr != null)
		{
			plyr.sendPacket(new SystemMessage(SystemMessage.CHATTING_IS_PERMITTED));
			plyr.updateNoChannel(0);
		}
		else
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE characters SET nochannel = ? WHERE obj_Id=?");
				statement.setLong(1, 0);
				statement.setInt(2, GetIDbyName(actor));
				statement.executeUpdate();
			}
			catch(Exception e)
			{
				res = false;
				_log.warn("Could not activate nochannel:" + e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}

		if(res)
			try
			{
				con = DatabaseFactory.getInstance().getConnection();

				statement = con.prepareStatement("DELETE FROM bans WHERE obj_id=?");
				statement.setInt(1, GetIDbyName(actor));
				statement.execute();
			}
			catch(Exception e)
			{
				_log.warn("could not store bans data:" + e);
				res = false;
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		return res;
	}
}
