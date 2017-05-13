package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.entity.Territory;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.territory.TerritoryWar;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2TerritoryWardInstance;
import ru.l2gw.gameserver.tables.ClanTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.HashMap;

/**
 * @author rage
 * @date 06.07.2010 11:46:06
 */
public class TerritoryWarManager
{
	private static TerritoryWarManager _instance;
	private static final Log _log = LogFactory.getLog("territory");

	public static final HashMap<Integer, Integer> badgesId = new HashMap<Integer, Integer>();
	static
	{
		badgesId.put(81, 13757);
		badgesId.put(82, 13758);
		badgesId.put(83, 13759);
		badgesId.put(84, 13760);
		badgesId.put(85, 13761);
		badgesId.put(86, 13762);
		badgesId.put(87, 13763);
		badgesId.put(88, 13764);
		badgesId.put(89, 13765);
	}

	private static final GArray<Territory> _territories = new GArray<Territory>(9);
	private static final TerritoryWar _territoryWar = new TerritoryWar();
	private static final FastMap<Integer, GCSArray<Integer>> _registeredClans = new FastMap<Integer, GCSArray<Integer>>().shared();
	private static final FastMap<Integer, GCSArray<Integer>> _registeredMerc = new FastMap<Integer, GCSArray<Integer>>().shared();

	public static void load()
	{
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		int clanCount = 0, mercCount = 0;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("SELECT * FROM territories ORDER BY territory_id");
			rset = stmt.executeQuery();
			while(rset.next())
			{
				Territory terr = new Territory(rset.getInt("territory_id"), rset.getInt("castle_id"), rset.getInt("fort_id"));
				String[] wards = rset.getString("ward_ids").split(";");
				for(String ward : wards)
					if(!ward.isEmpty())
					{
						int wardId = Integer.parseInt(ward);
						terr.addWardId(wardId);
						_log.info("TerritoryWarManager: " + ResidenceManager.getInstance().getCastleById(terr.getId() - 80).getName() + " Territory owned " + ResidenceManager.getInstance().getCastleById(wardId - 80).getName() + " Ward.");
					}

				_territories.add(terr);
				if(terr.getOwner() != null)
				{
					GCSArray<Integer> clans = _registeredClans.get(terr.getId());
					if(clans == null)
					{
						clans = new GCSArray<Integer>();
						_registeredClans.put(terr.getId(), clans);
					}
					terr.getOwner().setTerritoryId(terr.getId());
					clans.add(terr.getOwner().getClanId());
				}
			}

			stmt.close();
			rset.close();

			stmt = con.prepareStatement("SELECT * FROM `territory_reg` ORDER BY territory_id");
			rset = stmt.executeQuery();
			while(rset.next())
			{
				int terrId = rset.getInt("territory_id");
				int objectId = rset.getInt("object_id");
				boolean clanReg = rset.getInt("reg_type") == 1;

				if(clanReg)
				{
					L2Clan clan = ClanTable.getInstance().getClan(objectId);
					if(clan == null)
					{
						deleteReg(terrId, objectId);
						continue;
					}

					GCSArray<Integer> clans = _registeredClans.get(terrId);
					if(clans == null)
					{
						clans = new GCSArray<Integer>();
						_registeredClans.put(terrId, clans);
					}
					clanCount++;
					clan.setTerritoryId(terrId);
					clans.add(objectId);
				}
				else
				{
					GCSArray<Integer> mercs = _registeredMerc.get(terrId);
					if(mercs == null)
					{
						mercs = new GCSArray<Integer>();
						_registeredMerc.put(terrId, mercs);
					}
					mercCount++;
					mercs.add(objectId);
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("Error loading territories: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt, rset);
		}

		spawnTownNpc();
		_log.info("TerritoryWarManager: Loaded " + _territories.size() + " territories.");
		_log.info("TerritoryWarManager: Loaded " + clanCount + " clan and " + mercCount + " mercenaries registrations.");
		_territoryWar.correctWarDate();
		_territoryWar.startAutoTask();
	}

	public static void spawnTownNpc()
	{
		for(Territory terr : _territories)
			if(terr.getOwner() != null)
				terr.spawnNpc();
	}

	public static GArray<Territory> getTerritories()
	{
		return _territories;
	}

	public static Territory getTerritoryById(int terrId)
	{
		for(Territory terr : _territories)
			if(terr.getId() == terrId)
				return terr;

		return null;
	}

	public static Calendar getWarDate()
	{
		return _territoryWar.getWarDate();
	}

	public static TerritoryWar getWar()
	{
		return _territoryWar;
	}

	public static GArray<Integer> getRegisteredClans(int territoryId)
	{
		GArray<Integer> ret = new GArray<Integer>();
		if(_registeredClans.get(territoryId) != null)
			ret.addAll(_registeredClans.get(territoryId));

		return ret;
	}

	public static GArray<Integer> getRegisteredMerc(int territoryId)
	{
		GArray<Integer> ret = new GArray<Integer>();
		if(_registeredMerc.get(territoryId) != null)
			ret.addAll(_registeredMerc.get(territoryId));

		return ret;
	}

	public static int getClanRegisteredTerritoryId(int clanId)
	{
		for(int terrId : _registeredClans.keySet())
		{
			GCSArray<Integer> clans = _registeredClans.get(terrId);
			if(clans != null && clans.contains(clanId))
				return terrId;
		}
		return 0;
	}

	public static int getMercRegisteredTerritoryId(int objectId)
	{
		for(int terrId : _registeredMerc.keySet())
		{
			GCSArray<Integer> merc = _registeredMerc.get(terrId);
			if(merc != null && merc.contains(objectId))
				return terrId;
		}

		return 0;
	}

	public static void addMercRegistration(int terrId, int objectId)
	{
		GCSArray<Integer> merc = _registeredMerc.get(terrId);
		if(merc == null)
		{
			merc = new GCSArray<Integer>();
			_registeredMerc.put(terrId, merc);
		}

		if(!merc.contains(objectId))
		{
			merc.add(objectId);
			insertReg(terrId, objectId, 0);
		}
	}

	public static void removeMercRegistration(int terrId, int objectId)
	{
		GCSArray<Integer> merc = _registeredMerc.get(terrId);
		if(merc != null)
		{
			merc.remove(new Integer((objectId)));
			deleteReg(terrId, objectId);
		}
	}

	public static void addClanRegistration(int terrId, int clanId)
	{
		GCSArray<Integer> clans = _registeredClans.get(terrId);
		if(clans == null)
		{
			clans = new GCSArray<Integer>();
			_registeredClans.put(terrId, clans);
		}

		if(!clans.contains(clanId))
		{
			clans.add(clanId);
			insertReg(terrId, clanId, 1);
		}
	}

	public static void removeClanRegistration(int terrId, int clanId)
	{
		GCSArray<Integer> clans = _registeredClans.get(terrId);
		if(clans != null)
		{
			clans.remove(new Integer(clanId));
			deleteReg(terrId, clanId);
		}
	}

	private static void insertReg(int terrId, int objectId, int regType)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("INSERT INTO `territory_reg` VALUES(?, ?, ?)");
			stmt.setInt(1, terrId);
			stmt.setInt(2, objectId);
			stmt.setInt(3, regType);
			stmt.execute();
		}
		catch(Exception e)
		{
			_log.warn("TerritoryWar registration save error: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
		}
	}

	private static void deleteReg(int terrId, int objectId)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		_log.info("TerritoryWarManager: deleteReg: " + terrId + " " + objectId);
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("DELETE FROM `territory_reg` WHERE `territory_id` = ? and `object_id` = ?");
			stmt.setInt(1, terrId);
			stmt.setInt(2, objectId);
			stmt.execute();
		}
		catch(Exception e)
		{
			_log.warn("TerritoryWar registration delete error: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
		}
	}

	public static void clearRegistration(int terrId)
	{
		GCSArray<Integer> clans = new GCSArray<Integer>();
		L2Clan owner = getTerritoryById(terrId).getOwner();
		owner.setTerritoryId(terrId);
		clans.add(owner.getClanId());
		_registeredClans.put(terrId, clans);
		_registeredMerc.put(terrId, null);

		Connection con = null;
		PreparedStatement stmt = null;
		_log.info("TerritoryWarManager: clearReg: " + terrId);
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("DELETE FROM `territory_reg` WHERE territory_id = ?");
			stmt.setInt(1, terrId);
			stmt.execute();
		}
		catch(Exception e)
		{
			_log.warn("TerritoryWar clear registration error: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
		}
	}

	public static boolean isFortInWar(SiegeUnit fort)
	{
		if(!_territoryWar.isInProgress())
			return false;

		for(Territory terr : _territories)
			if(terr.getOwner() != null && terr.getFort().getId() == fort.getId())
				return true;

		return false;
	}

	public static void changeTerritoryOwner(int terrId, int clanId)
	{
		Territory terr = getTerritoryById(terrId);
		if(terr == null)
		{
			_log.info("TerritoryWarManager: change owner territory is null for id: " + terrId);
			return;
		}

		L2Clan oldOwner = terr.getOwner();
		boolean lord = terr.hasLord();
		GCSArray<Integer> clans = _registeredClans.get(terrId);

		if(clans == null)
		{
			clans = new GCSArray<Integer>();
			_registeredClans.put(terrId, clans);
		}

		if(oldOwner != null)
			clans.remove(new Integer(oldOwner.getClanId()));

		L2Clan clan = ClanTable.getInstance().getClan(clanId);
		terr.setOwner(clan);
		_log.info("TerritoryWarManager: change owner for territory: " + terr.getName() + " from " + oldOwner + " to " + terr.getOwner());

		if(clanId > 0)
		{
			for(int tId : _registeredClans.keySet())
			{
				GCSArray<Integer> clanRegs = _registeredClans.get(tId);
				if(clanRegs != null && clanRegs.contains(clanId))
				{
					clanRegs.remove(new Integer(clanId));
					deleteReg(tId, clanId);
				}
			}
			clans.add(clanId);
			clan.setTerritoryId(terrId);
		}
		if(terr.hasLord() != lord)
			changeTerritoryLord(terr);
	}

	public static void respawnWard(int objectId)
	{
		if(!_territoryWar.isInProgress())
			return;

		L2TerritoryWardInstance ward = null;
		for(L2TerritoryWardInstance w : _territoryWar.getWards())
			if(w.getOwnerId() == objectId)
			{
				ward = w;
				break;
			}

		if(ward != null)
		{
			_territoryWar.removeSpawnedWard(ward);
			ward.setOwnerId(0);
			ward.getSpawn().respawnNpc(ward);
		}
	}

	public static void removeCamps()
	{
		for(GCSArray<Integer> clans : _registeredClans.values())
			for(int clanId : clans)
			{
				L2Clan clan = ClanTable.getInstance().getClan(clanId);
				if(clan != null)
					clan.removeCamp();
			}
	}

	public static Territory getTerritoryByWardId(int wardId)
	{
		for(Territory terr : _territories)
			if(terr.getWards().contains(wardId))
				return terr;

		return null;
	}

	public static void changeTerritoryLord(Territory terr)
	{
		for(L2NpcInstance cha : L2ObjectsStorage.getAllNpcs())
			if(cha.isShowTag())
				cha.updateAbnormalEffect();
	}
}
