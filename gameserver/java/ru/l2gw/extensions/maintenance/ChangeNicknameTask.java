package ru.l2gw.extensions.maintenance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.instancemanager.MaintenanceManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.StringTokenizer;

import static ru.l2gw.gameserver.network.GameClient.deleteCharByObjId;

public class ChangeNicknameTask extends MaintenanceTask
{
	private static Log _log = LogFactory.getLog("maintenance");
	private String _lastResult = "";
	/*
	 * params format:
	 * charId:oldNickname:newNickname:priceItemId:priceItemCount
	 */
	@Override
	public boolean doTask(String params)
	{
		StringTokenizer st = new StringTokenizer(params, ":");
		int charId;
		int noobId;
		String oldNickname;
		String newNickname;
		int itemId = -1;
		int itemCount = 0;

		try
		{
			charId = Integer.parseInt(st.nextToken());
			oldNickname = st.nextToken();
			newNickname = st.nextToken();
			itemId = Integer.parseInt(st.nextToken());
			itemCount = Integer.parseInt(st.nextToken());
		}
		catch(Exception e)
		{
			_log.warn("ChangeNicknameTask: can't parse params: " + params);
			e.printStackTrace();
			return false;
		}
		noobId = preCheckConditions(charId, oldNickname, newNickname, itemId, itemCount);
		if(noobId > 0)
		{
			Connection con;
			PreparedStatement stmt = null;
			ResultSet rs = null;

			// Deleting level 1 character from server
			deleteCharByObjId(noobId);
			// Updating in characters table
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("UPDATE characters SET char_name = ? WHERE obj_Id = ?");
				stmt.setString(1, newNickname);
				stmt.setInt(2, charId);
				stmt.execute();
			}
			catch(Exception e)
			{
				_log.warn("ChangeNicknameTask: can't update sql:");
				e.printStackTrace();
				_lastResult = "error while updating table 1";
			}
			finally
			{
				DbUtils.closeQuietly(stmt, rs);
			}

			// Updating in items table
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("UPDATE items SET count = count - ? WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY'");
				stmt.setInt(1, itemCount);
				stmt.setInt(2, charId);
				stmt.setInt(3, itemId);
				stmt.execute();
			}
			catch(Exception e)
			{
				_log.warn("ChangeNicknameTask: can't update sql:");
				e.printStackTrace();
				_lastResult = "error while updating table 1";
			}
			finally
			{
				DbUtils.closeQuietly(stmt, rs);
			}

			if(!getLastResult().equalsIgnoreCase(""))
				return false;

			// Updating in character_friends
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("UPDATE character_friends o, characters c SET o.friend_name=c.char_name WHERE o.friend_id=c.obj_id AND o.friend_name <> c.char_name");
				stmt.execute();
			}
			catch(Exception e)
			{
				_log.warn("ChangeNicknameTask: can't update sql:");
				e.printStackTrace();
				_lastResult = "error while updating table 2";
			}
			finally
			{
				DbUtils.closeQuietly(stmt, rs);
			}

			// Updating in olymp_nobles
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("UPDATE olymp_nobles o, characters c SET o.char_name=c.char_name WHERE o.char_id=c.obj_id AND o.char_name <> c.char_name");
				stmt.execute();
			}
			catch(Exception e)
			{
				_log.warn("ChangeNicknameTask: can't update sql:");
				e.printStackTrace();
				_lastResult = "error while updating table 2";
			}
			finally
			{
				DbUtils.closeQuietly(stmt, rs);
			}

			// Updating in olymp_nobles_prev
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("UPDATE olymp_nobles_prev o, characters c SET o.char_name=c.char_name WHERE o.char_id=c.obj_id AND o.char_name <> c.char_name");
				stmt.execute();
			}
			catch(Exception e)
			{
				_log.warn("ChangeNicknameTask: can't update sql:");
				e.printStackTrace();
				_lastResult = "error while updating table 3";
			}
			finally
			{
				DbUtils.closeQuietly(stmt, rs);
			}

			// Updating in hero_history
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				stmt = con.prepareStatement("UPDATE hero_history o, characters c SET o.char_name=c.char_name WHERE o.char_id=c.obj_id AND o.char_name <> c.char_name");
				stmt.execute();
			}
			catch(Exception e)
			{
				_log.warn("ChangeNicknameTask: can't update sql:");
				e.printStackTrace();
				_lastResult = "error while updating table 4";
			}
			finally
			{
				DbUtils.closeQuietly(stmt, rs);
			}
		}
		else
		{
			_log.warn("ChangeNicknameTask: preCheckConditions failed for nickname change " + oldNickname + " -> " + newNickname + " char_id " + charId);
			return false;
		}

		return true;
	}

	private int preCheckConditions(int charId, String oldNickname, String newNickname, int itemId, int itemCount)
	{
		int _noobId;

		// 1. You are allowed to change a character's name only once every three months.
		// (should be implemented in account management web service)

		// 2. You must have required items in your character's inventory to complete rename task.
		if(itemCount > 0 && getCharacterItemCount(charId, itemId, "INVENTORY") < itemCount)
		{
			_lastResult = "Characters " + oldNickname + " doesn't have items to pay for rename. Task cannot be completed.";
			return -1;
		}

		// 3. You must create a new character with the desired name in order to reserve the name. This character must stay at Level 1, and must be on the same server as the character who will be receiving the name change. The old name cannot be used by anyone else (including yourself) on that server once the name is changed. The old name can never be used on that server again.
		_noobId = getNoobObjId(newNickname);
		if(_noobId <= 0 || getNoobLevel(_noobId) != 1)
		{
			_lastResult = "Character level 1 with nickname " + newNickname + " was not found. Task cannot be completed.";
			return -1;
		}
		// 4. You must remove the character who will be receiving a name change from his or her clan/alliances before you request a name change, and until the change is made.
		if(getCharClanId(charId) > 0)
		{
			_lastResult = "Character " + oldNickname + " is still in the clan. Task cannot be completed.";
			return -1;
		}

		// 5. Characters that have Hero status cannot change their name until they lose Hero status
		if(isHero(charId))
		{
			_lastResult = "Character " + oldNickname + " is hero. Task cannot be completed.";
			return -1;
		}
		return _noobId;
	}

	private int getCharacterItemCount(int charId, int itemId, String loc)
	{
		int count = 0;

		Connection con;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("SELECT count FROM items WHERE owner_id = ? AND item_id = ? and loc = ?");
			stmt.setInt(1, charId);
			stmt.setInt(2, itemId);
			stmt.setString(3, loc);
			rs = stmt.executeQuery();
			if(rs.next())
				count = rs.getInt(1);
			else
				count = -1;
		}
		catch(Exception e)
		{
			_log.warn("ChangeNicknameTask: (getCharacterItemCount) can't select from sql:");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(stmt, rs);
		}

		return count;
	}

	@Override
	public void addTask(String params)
	{
		StringTokenizer st = new StringTokenizer(params, ":");
		int charId;
		String oldNickname;
		String newNickname;

		Connection con = null;
		PreparedStatement stmt;
		PreparedStatement stmt2;
		ResultSet rs;

		try
		{
			charId = Integer.parseInt(st.nextToken());
			oldNickname = st.nextToken();
			newNickname = st.nextToken();
		}
		catch(NumberFormatException e)
		{
			_log.warn("ChangeNickname: (addTask) can't parse params: " + params);
			e.printStackTrace();
			return;
		}

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("SELECT * FROM maintenance_task WHERE name = 'ChangeNicknameTask' AND status = 0 and param like '" + charId + ":%'");
			rs = stmt.executeQuery();
			if(rs.next())
			{
				int id = rs.getInt(1);
				stmt2 = con.prepareStatement("DELETE FROM maintenance_task WHERE id = ?");
				stmt2.setInt(1, id);
				stmt2.execute();
				stmt2.close();
			}
			stmt.close();

			stmt = con.prepareStatement("REPLACE INTO maintenance_task(`name`, `param`, `status`, `result`, `datetime`) VALUES('ChangeNicknameTask', ?, 0, 0, ?)");
			stmt.setString(1, charId + ":" + oldNickname + ":" + newNickname);
			stmt.setInt(2, MaintenanceManager.getInstance().getMaintenanceTime());
			stmt.execute();
			stmt.close();
		}
		catch(Exception e)
		{
			_log.warn("ChangeNicknameTask: (addTask) can't update sql:");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}

	@Override
	public String getLastResult()
	{
		String ret = _lastResult;
		_lastResult = "";
		return ret;
	}

	public int getNoobObjId(String newNick)
	{
		Connection con;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = -1;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("SELECT obj_Id FROM characters WHERE char_name = ?");
			stmt.setString(1, newNick);
			rs = stmt.executeQuery();
			if(rs.next())
				result = rs.getInt(1);
			else
				result = -1;
		}
		catch(Exception e)
		{
			_log.warn("ChangeNicknameTask: (getNoobObjId) can't select from sql:");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(stmt, rs);
		}


		return result;
	}

	public int getNoobLevel(int objId)
	{
		Connection con;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = -1;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("SELECT level FROM character_subclasses WHERE char_obj_id = ? AND isBase = 1");
			stmt.setInt(1, objId);
			rs = stmt.executeQuery();
			if(rs.next())
				result = rs.getInt(1);
			else
				result = -1;
		}
		catch(Exception e)
		{
			_log.warn("ChangeNicknameTask: (getNoobLevel) can't select from sql:");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(stmt, rs);
		}


		return result;
	}

	private int getCharClanId(int charId)
	{
		Connection con;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = -1;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("SELECT clanid FROM characters WHERE obj_Id = ?");
			stmt.setInt(1, charId);
			rs = stmt.executeQuery();
			if(rs.next())
				result = rs.getInt(1);
			else
				result = -1;
		}
		catch(Exception e)
		{
			_log.warn("ChangeNicknameTask: (getCharClanId) can't select from sql:");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(stmt, rs);
		}


		return result;
	}

	private boolean isHero(int charId)
	{
		boolean result = false;

		Connection con;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("SELECT hh1.char_id  FROM hero_history hh1 LEFT OUTER JOIN hero_history hh2 ON (hh1.char_id = hh2.char_id) INNER JOIN characters c ON (c.obj_id = hh1.char_id) LEFT OUTER JOIN clan_data cd on (c.clanid = cd.clan_id) WHERE hh1.mons= ? AND hh1.char_id = ? GROUP BY 1");
			int mons = Integer.parseInt(String.format("%04d%02d", Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)));
			stmt.setInt(1, mons);
			stmt.setInt(2, charId);
			rs = stmt.executeQuery();
			result = rs.next();
		}
		catch(Exception e)
		{
			_log.warn("ChangeNicknameTask: (isHer) can't select from sql:");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(stmt, rs);
		}

		return result;
	}

}
