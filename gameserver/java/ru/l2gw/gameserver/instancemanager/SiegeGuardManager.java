package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class SiegeGuardManager
{
	private static Log _log = LogFactory.getLog(SiegeGuardManager.class.getName());

	private SiegeUnit _castle;
	private List<L2Spawn> _siegeGuardSpawn = new FastList<L2Spawn>();

	public SiegeGuardManager(SiegeUnit castle)
	{
		_castle = castle;
	}

	/**
	 * Add guard.<BR><BR>
	 */
	public void addSiegeGuard(L2Player player, int npcId)
	{
		if(player == null)
			return;
		addSiegeGuard(player.getLoc(), npcId);
	}

	/**
	 * Add guard.<BR><BR>
	 */
	public void addSiegeGuard(Location loc, int npcId)
	{
		saveSiegeGuard(loc, npcId, 0);
	}

	/**
	 * Hire merc.<BR><BR>
	 */
	public void hireMerc(L2Player player, int npcId)
	{
		if(player == null)
			return;
		hireMerc(player.getLoc(), npcId);
	}

	/**
	 * Hire merc.<BR><BR>
	 */
	public void hireMerc(Location loc, int npcId)
	{
		saveSiegeGuard(loc, npcId, 1);
	}

	/**
	 * Remove a single mercenary, identified by the npcId and location.
	 * Presumably, this is used when a castle lord picks up a previously dropped ticket
	 */
	public void removeMerc(int npcId, Location loc)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Delete From siege_guards Where npcId = ? And x = ? AND y = ? AND z = ? AND isHired = 1");
			statement.setInt(1, npcId);
			statement.setInt(2, loc.getX());
			statement.setInt(3, loc.getY());
			statement.setInt(4, loc.getZ());
			statement.execute();
		}
		catch(Exception e1)
		{
			_log.warn("Error deleting hired siege guard at " + loc.toString() + ":" + e1);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Remove mercs.<BR><BR>
	 */
	public static void removeMercsFromDb(int castleId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Delete From siege_guards Where unitId = ? And isHired = 1");
			statement.setInt(1, castleId);
			statement.execute();
		}
		catch(Exception e1)
		{
			_log.warn("Error deleting hired siege guard for castle " + castleId + ":" + e1);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Spawn guards.<BR><BR>
	 */
	public void spawnSiegeGuard()
	{
		unspawnSiegeGuard();
		loadSiegeGuard();
		for(L2Spawn spawn : _siegeGuardSpawn)
			if(spawn != null)
			{
				spawn.init();
				if(spawn.getRespawnTime() == 0)
					spawn.stopRespawn();
			}
	}

	/**
	 * Unspawn guards.<BR><BR>
	 */
	public void unspawnSiegeGuard()
	{
		for(L2Spawn spawn : _siegeGuardSpawn)
		{
			if(spawn == null)
				continue;

			spawn.stopRespawn();
			if(spawn.getLastSpawn() != null)
				spawn.getLastSpawn().deleteMe();
		}

		getSiegeGuardSpawn().clear();
	}

	/**
	 * Load guards.<BR><BR>
	 */
	private void loadSiegeGuard()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM siege_guards Where unitId = ? And isHired = ?");
			statement.setInt(1, getCastle().getId());
			if(getCastle().getOwnerId() > 0) // If castle is owned by a clan, then don't spawn default guards
				statement.setInt(2, 1);
			else
				statement.setInt(2, 0);
			rset = statement.executeQuery();

			L2Spawn spawn1;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getTemplate(rset.getInt("npcId"));
				if(template1 != null)
				{
					spawn1 = new L2Spawn(template1);
					spawn1.setId(rset.getInt("id"));
					spawn1.setAmount(1);
					spawn1.setLocx(rset.getInt("x"));
					spawn1.setLocy(rset.getInt("y"));
					spawn1.setLocz(rset.getInt("z"));
					spawn1.setHeading(rset.getInt("heading"));
					spawn1.setRespawnDelay(rset.getInt("respawnDelay"));
					spawn1.setLocation(0);

					_siegeGuardSpawn.add(spawn1);
				}
				else
					_log.warn("Missing npc data in npc table for id: " + rset.getInt("npcId"));
			}
		}
		catch(Exception e1)
		{
			_log.warn("Error loading siege guard for castle " + getCastle().getName() + ":" + e1);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	/**
	 * Save guards.<BR><BR>
	 */
	private void saveSiegeGuard(Location loc, int npcId, int isHire)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Insert Into siege_guards (unitId, npcId, x, y, z, heading, respawnDelay, isHired) Values (?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, getCastle().getId());
			statement.setInt(2, npcId);
			statement.setInt(3, loc.getX());
			statement.setInt(4, loc.getY());
			statement.setInt(5, loc.getZ());
			statement.setInt(6, loc.getHeading());
			if(isHire == 1)
				statement.setInt(7, 0);
			else
				statement.setInt(7, 600);
			statement.setInt(8, isHire);
			statement.execute();
		}
		catch(Exception e1)
		{
			_log.warn("Error adding siege guard for castle " + getCastle().getName() + ":" + e1);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public final SiegeUnit getCastle()
	{
		return _castle;
	}

	public final List<L2Spawn> getSiegeGuardSpawn()
	{
		return _siegeGuardSpawn;
	}
}