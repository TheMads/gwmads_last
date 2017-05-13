package ru.l2gw.gameserver.idfactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class IdFactory
{
	private static Log _log = LogFactory.getLog(IdFactory.class.getName());

	protected boolean initialized;

	protected long releasedCount = 0;

	public static final int FIRST_OID = 0x10000000;
	public static final int LAST_OID = 0x7FFFFFFF;
	public static final int FREE_OBJECT_ID_SIZE = LAST_OID - FIRST_OID;

	protected static final IdFactory _instance = new BitSetIDFactory();

	protected IdFactory()
	{
		setAllCharacterOffline();
		cleanUpDB();
	}

	private void setAllCharacterOffline()
	{
		Connection conn = null;
		Statement stmt = null;
		try
		{
			conn = DatabaseFactory.getInstance().getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("UPDATE characters SET online = 0");
			stmt.executeUpdate("UPDATE characters SET accesslevel = 0 WHERE accesslevel = -1");
			_log.info("Clear characters online status and accesslevel.");
		}
		catch(SQLException e)
		{}
		finally
		{
			DbUtils.closeQuietly(conn, stmt);
		}
	}

	/**
	 * Cleans up Database
	 */
	private void cleanUpDB()
	{
		Connection conn = null;
		Statement stmt = null;
		try
		{
			int cleanCount = 0;
			int curCount;

			conn = DatabaseFactory.getInstance().getConnection();
			stmt = conn.createStatement();

			if((curCount = stmt.executeUpdate("DELETE FROM character_friends WHERE character_friends.char_id NOT IN (SELECT obj_Id FROM characters) OR character_friends.friend_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_friends.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_contactlist WHERE character_contactlist.char_id NOT IN (SELECT obj_Id FROM characters) OR character_contactlist.contact_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_contactlist.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM couples WHERE couples.player1Id NOT IN (SELECT obj_Id FROM characters) OR couples.player2Id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table couples.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_blocklist WHERE character_blocklist.obj_Id NOT IN (SELECT obj_Id FROM characters) OR character_blocklist.target_Id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_friends.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_hennas WHERE character_hennas.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_hennas.");
			}

			if((curCount = stmt.executeUpdate("DELETE FROM character_macroses WHERE character_macroses.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_macroses.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_quests WHERE character_quests.char_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_quests.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM character_recipebook WHERE character_recipebook.char_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_recipebook.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_shortcuts WHERE character_shortcuts.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_shortcuts.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM character_skills WHERE character_skills.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_skills.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_effects_save WHERE character_effects_save.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_effects_save.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_skills_save WHERE character_skills_save.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_skills_save.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM character_subclasses WHERE character_subclasses.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_subclasses.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_variables WHERE character_variables.obj_id = '0';")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_variables.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM clan_data WHERE clan_data.leader_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table clan_data.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM clan_subpledges WHERE clan_subpledges.clan_id NOT IN (SELECT clan_id FROM clan_data);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table clan_subpledges.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM ally_data WHERE ally_data.leader_id NOT IN (SELECT clan_id FROM clan_data);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table ally_data.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM pets WHERE pets.item_obj_id NOT IN (SELECT object_id FROM items);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table pets.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM siege_clans WHERE siege_clans.clan_id NOT IN (SELECT clan_id FROM clan_data);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table siege_clans.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM items WHERE owner_id NOT IN (SELECT obj_Id FROM characters) AND owner_id NOT IN (SELECT clan_id FROM clan_data) AND owner_id NOT IN (SELECT objId FROM pets) AND owner_id NOT IN (SELECT id FROM npc);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table items.");
			}
						if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM `character_variables` WHERE `obj_id` NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_variables.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM `clan_wars` where clan1 not in (select clan_id FROM clan_data) or clan2 not in (select clan_id FROM clan_data);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table clan_wars.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM `augmentations` where item_id not in (select object_id FROM items);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table augmentations.");
			}

			if((curCount = stmt.executeUpdate("UPDATE characters SET clanid=0,pledge_type=0,pledge_rank=0,lvl_joined_academy=0,apprentice=0 WHERE clanid!=0 AND clanid NOT IN (SELECT clan_id FROM clan_data);")) > 0)
				_log.info("Updated " + curCount + " elements from table characters.");
			if((curCount = stmt.executeUpdate("UPDATE clan_data SET ally_id=0 WHERE ally_id!=0 AND ally_id NOT IN (SELECT ally_id FROM ally_data);")) > 0)
				_log.info("Updated " + curCount + " elements from table clan_data");
		    if((curCount = stmt.executeUpdate("UPDATE clanhall SET ownerId=0 WHERE ownerId!=0 AND ownerId NOT IN (SELECT clan_id FROM clan_data);")) > 0)
				_log.info("Updated " + curCount + " elements from table clanhall.");

			_log.info("Total cleaned " + cleanCount + " elements from database.");
		}
		catch(SQLException e)
		{}
		finally
		{
			DbUtils.closeQuietly(conn, stmt);
		}
	}

	/**
	 * @param con
	 * @return
	 * @throws SQLException
	 */
	protected int[] extractUsedObjectIDTable() throws SQLException
	{
		Connection con = null;
		Statement s = null;
		ResultSet result = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			//create a temporary table
			s = con.createStatement();
			try
			{
				s.executeUpdate("delete from temporaryObjectTable");
			}
			catch(SQLException e)
			{}

			s.addBatch("create table IF NOT EXISTS temporaryObjectTable (object_id int NOT NULL PRIMARY KEY) ENGINE = MEMORY");

			s.addBatch("insert into temporaryObjectTable (object_id) select obj_id from characters");
			s.addBatch("insert into temporaryObjectTable (object_id) select object_id from items");
			s.addBatch("insert into temporaryObjectTable (object_id) select clan_id from clan_data");
			s.addBatch("insert into temporaryObjectTable (object_id) select ally_id from ally_data");
			s.addBatch("insert into temporaryObjectTable (object_id) select objId from pets");
			s.addBatch("insert into temporaryObjectTable (object_id) select id from couples");
			s.executeBatch();

			result = s.executeQuery("select count(object_id) from temporaryObjectTable");

			result.next();
			int size = result.getInt(1);
			int[] tmp_obj_ids = new int[size];
			// System.out.println("tmp table size: " + tmp_obj_ids.length);
			DbUtils.closeQuietly(result);

			result = s.executeQuery("select object_id from temporaryObjectTable ORDER BY object_id");

			int idx = 0;
			while(result.next())
				tmp_obj_ids[idx++] = result.getInt(1);

			return tmp_obj_ids;
		}
		finally
		{
			DbUtils.closeQuietly(con, s, result);
		}
	}

	public boolean isInitialized()
	{
		return initialized;
	}

	public static IdFactory getInstance()
	{
		return _instance;
	}

	public abstract int getNextId();

	/**
	 * return a used Object ID back to the pool
	 * @param object ID
	 */
	public void releaseId(@SuppressWarnings("unused") int id)
	{
		releasedCount++;
	}

	public long getReleasedCount()
	{
		return releasedCount;
	}

	public abstract int size();
}
