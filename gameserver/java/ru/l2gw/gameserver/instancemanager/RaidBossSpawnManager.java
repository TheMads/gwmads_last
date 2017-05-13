package ru.l2gw.gameserver.instancemanager;

import gnu.trove.map.hash.TIntIntHashMap;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.database.mysql;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.instances.L2RaidBossInstance;
import ru.l2gw.gameserver.tables.GmListTable;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.SpawnTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@SuppressWarnings({"nls", "unqualified-field-access", "boxing"})
public class RaidBossSpawnManager
{

	private static Log _log = LogFactory.getLog(RaidBossSpawnManager.class.getName());

	private static RaidBossSpawnManager _instance;

	protected static Map<Integer, L2RaidBossInstance> _bosses;
	protected static Map<Integer, L2Spawn> _spawntable;
	protected static Map<Integer, ScheduledFuture<?>> _schedules;

	protected static ConcurrentHashMap<Integer, PlayerRaidPoints> _points;

	public static enum StatusEnum
	{
		ALIVE,
		DEAD,
		UNDEFINED
	}

	private RaidBossSpawnManager()
	{
		_instance = this;
		if(!Config.DONTLOADSPAWN)
			reloadBosses();
	}

	public void reloadBosses()
	{
		fillSpawnTable();
		loadPlayerPoints();
	}

	public static RaidBossSpawnManager getInstance()
	{
		if(_instance == null)
			new RaidBossSpawnManager();
		return _instance;
	}

	private void loadPlayerPoints()
	{
		_points = new ConcurrentHashMap<>();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT * FROM `character_rbp` ORDER BY obj_id, boss_id");
			rset = statement.executeQuery();
			while(rset.next())
			{
				PlayerRaidPoints prp = _points.get(rset.getInt("obj_id"));
				if(prp == null)
				{
					prp = new PlayerRaidPoints(rset.getInt("obj_id"));
					_points.put(rset.getInt("obj_id"), prp);
				}
				prp.addPoints(rset.getInt("boss_id"), rset.getInt("points"));
			}

			for(PlayerRaidPoints prp : _points.values())
				prp.setModified(false);
		}
		catch(SQLException e)
		{
			_log.warn("RaidBossSpawnManager: Couldnt load player raidboss points");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private void fillSpawnTable()
	{
		_bosses = new FastMap<Integer, L2RaidBossInstance>();
		_schedules = new FastMap<Integer, ScheduledFuture<?>>();
		_spawntable = new FastMap<Integer, L2Spawn>();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT s.spawn_id, count, s.npc_templateid, s.locx, s.locy, s.locz, heading, respawn_delay, respawn_random, s.loc_id, ks.respawn_time, periodOfDay, ks.current_hp, ks.current_mp FROM `spawnlist` s LEFT JOIN `kill_status` AS ks ON(s.spawn_id = ks.spawn_id) WHERE s.npc_templateid IN (SELECT id FROM `npc` WHERE type='L2RaidBoss' OR type='L2Boss' OR type='QueenAnt') and s.event_name is NULL ORDER by s.npc_templateid");
			rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			int npcId;
			int statuses = 0;
			while(rset.next())
			{
				npcId = rset.getInt("npc_templateid");
				template1 = getValidTemplate(npcId);
				if(template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setId(rset.getInt("spawn_id"));
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					spawnDat.setRespawnRandom(rset.getInt("respawn_random"));
					if(rset.getString("respawn_time") == null)
						spawnDat.setRespawnTime(0);
					else
						spawnDat.setRespawnTime(rset.getInt("respawn_time"));
					spawnDat.setLocation(rset.getInt("loc_id"));

					StatsSet info = null;
					if(rset.getInt("current_hp") > 0)
					{
						info = new StatsSet();
						info.set("current_hp", rset.getInt("current_hp"));
						info.set("current_mp", rset.getInt("current_mp"));
						statuses++;
					}

					addNewSpawn(spawnDat, false, info);
				}
				else
					_log.warn("RaidBossSpawnManager: Could not load raidboss #" + npcId + " from DB");
				mysql.set("DELETE FROM `kill_status` WHERE `respawn_time` < " + System.currentTimeMillis() / 1000);
			}
			_log.info("RaidBossSpawnManager: Loaded " + statuses + " Statuses");
			_log.info("RaidBossSpawnManager: Loaded " + _bosses.size() + " Instances");
			_log.info("RaidBossSpawnManager: Scheduled " + _schedules.size() + " Instances");
		}
		catch(SQLException e)
		{
			_log.warn("RaidBossSpawnManager: Couldnt load raidboss spawnlist");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void addPoints(int objectId, int bossId, int points)
	{
		if(!_points.containsKey(objectId))
			_points.put(objectId, new PlayerRaidPoints(objectId));

		_points.get(objectId).addPoints(bossId, points);
	}

	public void updateStatus(L2RaidBossInstance raidboss, boolean isBossDead)
	{
		if(raidboss.getSpawn() == null)
			return;

		if(isBossDead)
		{
			raidboss.setRaidStatus(StatusEnum.DEAD);
			raidboss.getSpawn().updateInDb();

			long respawn_delay = raidboss.getSpawn().getRespawnTime() * 1000L - System.currentTimeMillis();
			if(respawn_delay > 0)
			{
				if(_schedules.get(raidboss.getNpcId()) != null)
				{
					_log.info("RaidBossSpawnManager: warning! " + raidboss.getName() + " has already scheduled!");
					_schedules.get(raidboss.getNpcId()).cancel(true);
					_schedules.remove(raidboss.getNpcId());
				}

				ScheduledFuture<?> futureSpawn = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnSchedule(raidboss.getNpcId()), respawn_delay);
				_schedules.put(raidboss.getNpcId(), futureSpawn);

				_log.info("RaidBossSpawnManager: Scheduled " + raidboss.getName() + " for respawn in " + Util.formatTime(respawn_delay / 1000));
			}
			else
				_log.info("RaidBossSpawnManager: warning! " + raidboss.getName() + " has worng respawnDelay: " + respawn_delay);
		}
		else
			raidboss.setRaidStatus(StatusEnum.ALIVE);
	}

	private class SpawnSchedule implements Runnable
	{
		private int bossId;

		public SpawnSchedule(int npcId)
		{
			bossId = npcId;
		}

		public void run()
		{
			L2RaidBossInstance raidboss;

			if(bossId == 25328)
				raidboss = DayNightSpawnManager.getInstance().handleNightBoss(_spawntable.get(bossId));
			else
				raidboss = (L2RaidBossInstance) _spawntable.get(bossId).doSpawn(true);

			if(raidboss != null)
			{
				raidboss.setRaidStatus(StatusEnum.ALIVE);
				GmListTable.broadcastMessageToGMs("Spawning RaidBoss " + raidboss.getName());
				_log.info("RaidBossSpawnManager: Spawning RaidBoss " + raidboss.getName());

				_bosses.put(bossId, raidboss);
			}
			_schedules.remove(bossId);
		}
	}

	public void addNewSpawn(L2Spawn spawnDat, boolean storeInDb, StatsSet info)
	{
		if(spawnDat == null)
			return;

		int bossId = spawnDat.getNpcId();
		if(_spawntable.containsKey(bossId))
			return;

		SpawnTable.getInstance().addNewSpawn(spawnDat, storeInDb, null);

		if(System.currentTimeMillis() > spawnDat.getRespawnTime() * 1000L)
		{
			L2RaidBossInstance raidboss = null;

			if(bossId == 25328)
				raidboss = DayNightSpawnManager.getInstance().handleNightBoss(spawnDat);
			else
				raidboss = (L2RaidBossInstance) spawnDat.doSpawn(true);

			if(raidboss != null)
			{
				if(info != null)
				{
					if(info.getInteger("current_hp") != raidboss.getMaxHp() || info.getInteger("current_mp") != raidboss.getMaxMp())
					{
						raidboss.setCurrentHp(info.getInteger("current_hp"));
						raidboss.setCurrentMp(info.getInteger("current_mp"));
					}
				}
				raidboss.setRaidStatus(StatusEnum.ALIVE);

				_bosses.put(bossId, raidboss);
			}
		}
		else
		{
			long spawnTime = spawnDat.getRespawnTime() * 1000L - System.currentTimeMillis();
			ScheduledFuture<?> futureSpawn = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnSchedule(bossId), spawnTime);
			_schedules.put(bossId, futureSpawn);
		}

		_spawntable.put(bossId, spawnDat);
	}

	public void deleteSpawn(L2Spawn spawnDat, boolean updateDb)
	{
		if(spawnDat == null)
			return;
		if(!_spawntable.containsKey(spawnDat.getNpcId()))
			return;

		int bossId = spawnDat.getNpcId();

		SpawnTable.getInstance().deleteSpawn(spawnDat, updateDb);
		_spawntable.remove(bossId);

		if(_bosses.containsKey(bossId))
			_bosses.remove(bossId);

		if(_schedules.containsKey(bossId))
		{
			ScheduledFuture<?> f = _schedules.get(bossId);
			f.cancel(true);
			_schedules.remove(bossId);
		}

		if(updateDb)
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM kill_status WHERE npc_templateid=?");
				statement.setInt(1, bossId);
				statement.execute();
			}
			catch(Exception e)
			{
				// problem with deleting spawn
				_log.warn("RaidBossSpawnManager: Could not remove raidboss #" + bossId + " from DB: " + e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	private void updateStatusDb()
	{
		Connection con = null;
		PreparedStatement statement = null;

		for(Integer bossId : _bosses.keySet())
			try
			{
				con = DatabaseFactory.getInstance().getConnection();

				L2RaidBossInstance raidboss = _bosses.get(bossId);

				if(raidboss == null)
					continue;

				if(raidboss.getRaidStatus().equals(StatusEnum.ALIVE) && (raidboss.getCurrentHp() != raidboss.getMaxHp() || raidboss.getCurrentMp() != raidboss.getMaxMp()))
				{
					statement = con.prepareStatement("REPLACE INTO `kill_status` (spawn_id, npc_templateid, current_hp, current_mp, respawn_time) VALUES (?,?,?,?,0)");
					statement.setInt(1, raidboss.getSpawn().getId());
					statement.setInt(2, bossId);
					statement.setInt(3, (int) raidboss.getCurrentHp());
					statement.setInt(4, (int) raidboss.getCurrentMp());
					statement.execute();
					statement.close();
				}
				else if(!Config.RAID_FORCE_STATUS_UPDATE && raidboss.getRaidStatus().equals(StatusEnum.DEAD))
				{
					statement = con.prepareStatement("REPLACE INTO `kill_status` (spawn_id, npc_templateid, current_hp, current_mp, respawn_time) VALUES (?,?,0,0,?)");
					statement.setInt(1, raidboss.getSpawn().getId());
					statement.setInt(2, bossId);
					statement.setInt(3, raidboss.getSpawn().getRespawnTime());
					statement.execute();
					statement.close();
				}
			}
			catch(SQLException e)
			{
				_log.warn("RaidBossSpawnManager: Couldnt update kill_status table");
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
	}

	private void updatePointsDb()
	{
		Connection con = null;
		PreparedStatement statement;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			for(PlayerRaidPoints prp : _points.values())
			{
				if(prp.isModified())
				{
					for(int bossId : prp.getPoints().keys())
					{
						statement = con.prepareStatement("REPLACE INTO `character_rbp` VALUES (?,?,?)");
						statement.setInt(1, prp.objectId);
						statement.setInt(2, bossId);
						statement.setInt(3, prp.getPoints().get(bossId));
						statement.execute();
						statement.close();
					}
					prp.setModified(false);
				}
			}
		}
		catch(SQLException e)
		{
			_log.warn("RaidBossSpawnManager: Couldnt update player raid boss points table");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}

	public String[] getAllRaidBossStatus()
	{
		String[] msg = new String[_bosses == null ? 0 : _bosses.size()];

		if(_bosses == null)
		{
			msg[0] = "None";
			return msg;
		}

		int index = 0;

		for(Integer i : _bosses.keySet())
		{
			L2RaidBossInstance raidboss = _bosses.get(i);

			msg[index] = raidboss.getName() + ": " + raidboss.getRaidStatus().name();
			index++;
		}

		return msg;
	}

	public String getRaidBossStatus(int bossId)
	{
		String msg = "RaidBoss Status....\n";

		if(_bosses == null)
		{
			msg += "None";
			return msg;
		}

		if(_bosses.containsKey(bossId))
		{
			L2RaidBossInstance raidboss = _bosses.get(bossId);

			msg += raidboss.getName() + ": " + raidboss.getRaidStatus().name();
		}

		return msg;
	}

	public StatusEnum getRaidBossStatusId(int bossId)
	{
		if(_bosses.containsKey(bossId))
			return _bosses.get(bossId).getRaidStatus();
		else if(_schedules.containsKey(bossId))
			return StatusEnum.DEAD;
		else
			return StatusEnum.UNDEFINED;
	}

	public L2NpcTemplate getValidTemplate(int bossId)
	{
		L2NpcTemplate template = NpcTable.getTemplate(bossId);
		if(template == null)
			return null;
		if(!template.type.equalsIgnoreCase("L2RaidBoss") && !template.type.equalsIgnoreCase("L2Boss") && !template.type.equalsIgnoreCase("QueenAnt"))
			return null;
		return template;
	}

	public void notifySpawnNightBoss(L2RaidBossInstance raidboss)
	{
		raidboss.setRaidStatus(StatusEnum.ALIVE);
		_bosses.put(raidboss.getNpcId(), raidboss);
		GmListTable.broadcastMessageToGMs("Spawning night RaidBoss " + raidboss.getName());
	}

	public boolean isDefined(int bossId)
	{
		return _spawntable.containsKey(bossId);
	}

	public Map<Integer, L2RaidBossInstance> getBosses()
	{
		return _bosses;
	}

	public L2RaidBossInstance getBoss(int bossId)
	{
		return _bosses.get(bossId);
	}

	public Map<Integer, L2Spawn> getSpawnTable()
	{
		return _spawntable;
	}

	public PlayerRaidPoints getPointsByOwnerId(int ownerId)
	{
		recalculatePointsRank();
		return _points.get(ownerId);
	}

	long _lastPointsStore = 0;

	public void recalculatePointsRank()
	{
		if(_lastPointsStore < System.currentTimeMillis())
		{
			_lastPointsStore = System.currentTimeMillis() + 300000;
			updatePointsDb();

			ArrayList<PlayerRaidPoints> list = new ArrayList<>(_points.values());
			Collections.sort(list);
			for(int i = 0; i < list.size(); i++)
				list.get(i).setRank(i + 1);
		}
	}

	/**
	 * Saves all raidboss status and then clears all info from memory,
	 * including all schedules.
	 */
	public void cleanUp()
	{
		if(_schedules != null)
		{
			for(Integer bossId : _schedules.keySet())
			{
				ScheduledFuture<?> f = _schedules.get(bossId);
				f.cancel(true);
			}
			_schedules.clear();
		}

	}

	public boolean isScheduled(int bossId)
	{
		return _schedules.containsKey(bossId);
	}

	public void saveData()
	{
		updateStatusDb();
		updatePointsDb();
		_log.debug("RaidBossSpawnManager: All raidboss info saved!");
	}

	public class PlayerRaidPoints implements Comparable<PlayerRaidPoints>
	{
		public final int objectId;
		private int rank = 0;
		private final TIntIntHashMap points;
		private int totalPoints = 0;
		private boolean modified;

		public PlayerRaidPoints(int _objectId)
		{
			objectId = _objectId;
			points = new TIntIntHashMap();
			modified = true;
		}

		public int getTotalPoints()
		{
			return totalPoints;
		}

		public void setRank(int rank)
		{
			this.rank = rank;
		}

		public int getRank()
		{
			return rank;
		}

		public void addPoints(int bossId, int rbp)
		{
			totalPoints += rbp;
			if(points.containsKey(bossId))
				points.put(bossId, points.get(bossId) + rbp);
			else
				this.points.put(bossId, rbp);

			modified = true;
		}

		public TIntIntHashMap getPoints()
		{
			return points;
		}

		public void setModified(boolean modified)
		{
			this.modified = modified;
		}

		public boolean isModified()
		{
			return modified;
		}

		@Override
		public int compareTo(PlayerRaidPoints prp)
		{
			return prp.getTotalPoints() - getTotalPoints();
		}
	}
}
