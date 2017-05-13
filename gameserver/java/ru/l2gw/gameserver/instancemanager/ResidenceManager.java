package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.tables.DoorTable;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class ResidenceManager
{
	private static ResidenceManager _instance;
	protected static Log _log = LogFactory.getLog(ResidenceManager.class.getName());
	private static HashMap<Integer, GArray<L2Zone>> zones = new HashMap<Integer, GArray<L2Zone>>();
	private static GArray<ClanHall> clanHallList = new GArray<ClanHall>();
	private static GArray<Fortress> fortressList = new GArray<Fortress>();
	private static GArray<Castle> castleList = new GArray<Castle>();
	private static HashMap<Integer, SiegeUnit> unitsById = new HashMap<Integer, SiegeUnit>();

	public static ResidenceManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new ResidenceManager();
			_instance.load();
			_instance.loadDoor();
		}
		return _instance;
	}

	public void addZone(L2Zone zone)
	{
		if(zones.get(zone.getEntityId()) == null)
			zones.put(zone.getEntityId(), new GArray<L2Zone>());
		zones.get(zone.getEntityId()).add(zone);
	}

	public void clearAllZones()
	{
		zones.clear();
	}

	/**
	 * Возвращает резиденцию, соответствующую индексу.
	 */
	public SiegeUnit getBuildingById(int unitId)
	{
		return unitsById.get(unitId);
	}

	/**
	 * Возвращает замок, соответствующую индексу.
	 */
	public Castle getCastleById(int unitId)
	{
		for(Castle castle : castleList)
			if(castle.getId() == unitId)
				return castle;
		return null;
	}

	/**
	 * Возвращает кланхол, соответствующую индексу.
	 */
	public ClanHall getClanHallById(int unitId)
	{
		for(ClanHall clanHall : clanHallList)
			if(clanHall.getId() == unitId)
				return clanHall;
		return null;
	}

	/**
	 * Если объект находиться в зоне резиденции, возвращает эту резиденцию.
	 * Иначе возвращает null.
	 */
	public SiegeUnit getBuildingByObject(L2Object activeObject)
	{
		return getBuildingByCoord(activeObject.getX(), activeObject.getY());
	}

	/**
	 * Если объект находиться в зоне осады резиденции, возвращает эту резиденцию.
	 * Иначе возвращает null.
	 */
	@SuppressWarnings("unused")
	private SiegeUnit getBuildingByObjectInSiegeZone(L2Object activeObject)
	{
		return getBuildingBySiegeZoneCoord(activeObject.getX(), activeObject.getY());
	}

	/**
	 * Если объект находиться в зоне осады замка, возвращает этот замок.
	 * Иначе возвращает null.
	 */
	public Castle getCastleByObjectInSiegeZone(L2Object activeObject)
	{
		return getCastleBySiegeZoneCoord(activeObject.getX(), activeObject.getY());
	}

	/**
	 * Если координаты принадлежат осадной зоне кланхола, возвращает этот кланхол.
	 * Иначе возвращает null.
	 */
	public ClanHall getClanHallByObjectInSiegeZone(L2Object activeObject)
	{
		return getClanHallBySiegeZoneCoord(activeObject.getX(), activeObject.getY());
	}

	/**
	 * Если координаты принадлежат осадной зоне форта, возвращает этот форт.
	 * Иначе возвращает null.
	 */
	public Fortress getFortressByObjectInSiegeZone(L2Object activeObject)
	{
		return getFortressBySiegeZoneCoord(activeObject.getX(), activeObject.getY());
	}

	/**
	 * Если обьект находится в осадной зоне кланхола возвращает этот кланхол
	 * Иначе возвращает null.
	 */
	public ClanHall getClanHallBySiegeZoneCoord(int x, int y)
	{
		for(ClanHall unit : clanHallList)
			if(unit.checkIfInSiegeZone(x, y))
				return unit;
		return null;
	}

	/**
	 * Если координаты принадлежат осадной зоне форта  возвращает этот форт.
	 * Иначе возвращает null.
	 */
	public Fortress getFortressBySiegeZoneCoord(int x, int y)
	{
		for(Fortress unit : fortressList)
			if(unit.checkIfInSiegeZone(x, y))
				return unit;

		return null;
	}

	/**
	 * Если координаты принадлежат зоне осадной замка возвращает этот замок.
	 * Иначе возвращает null.
	 */
	private Castle getCastleBySiegeZoneCoord(int x, int y)
	{
		for(Castle unit : castleList)
			if(unit.checkIfInSiegeZone(x, y))
				return unit;
		return null;
	}

	/**
	 * Если обьект находится в зоне резиденции возвращает эту резиденцию.
	 * Иначе возвращает null.
	 */
	private SiegeUnit getBuildingByCoord(int x, int y)
	{
		try
		{
			for(SiegeUnit unit : unitsById.values())
				if(unit.checkIfInZone(x, y))
					return unit;
		}
		catch(NullPointerException npe)
		{
		}

		return null;
	}

	/**
	 * Если обьект находится в зоне резиденции возвращает эту резиденцию.
	 * Иначе возвращает null.
	 */
	private SiegeUnit getClanHallByCoord(int x, int y)
	{
		try
		{
			for(SiegeUnit unit : clanHallList)
				if(unit.checkIfInZone(x, y))
					return unit;
		}
		catch(NullPointerException npe)
		{
		}

		return null;
	}

	/**
	 * Если обьект находится в зоне резиденции возвращает эту резиденцию.
	 * Иначе возвращает null.
	 */
	private SiegeUnit getFortressByCoord(int x, int y)
	{
		try
		{
			for(SiegeUnit unit : fortressList)
				if(unit.checkIfInZone(x, y))
					return unit;
		}
		catch(NullPointerException npe)
		{
		}

		return null;
	}

	/**
	 * Если обьект находится в зоне резиденции возвращает эту резиденцию.
	 * Иначе возвращает null.
	 */
	@SuppressWarnings("unused")
	private SiegeUnit getCastleByCoord(int x, int y)
	{
		try
		{
			for(SiegeUnit unit : unitsById.values())
				if(unit.checkIfInZone(x, y))
					return unit;
		}
		catch(NullPointerException npe)
		{
		}

		return null;
	}

	/**
	 * Если обьект находится в зоне резиденции возвращает эту резиденцию.
	 * Иначе возвращает null.
	 */
	public SiegeUnit getBuildingBySiegeZoneCoord(int x, int y)
	{
		try
		{
			for(SiegeUnit unit : unitsById.values())
				if(unit.getSiegeZone() != null && unit.getSiegeZone().isInsideZone(x, y))
					return unit;
		}
		catch(NullPointerException npe)
		{
		}

		return null;
	}

	/**
	 * Если обьект находится в зоне сиедж резиденции возвращает эту резиденцию.
	 * Иначе возвращает null.
	 */
	public SiegeUnit getBuildingByResidenceCoord(int x, int y)
	{
		try
		{
			for(SiegeUnit unit : unitsById.values())
				if(unit.checkIfInresidenceZone(x, y))
					return unit;
		}
		catch(NullPointerException npe)
		{
		}

		return null;
	}

	/**
	 * Возвращает список, содержащий все кланхолы
	 */
	public GArray<ClanHall> getClanHallList()
	{
		return clanHallList;
	}

	/**
	 * Возвращает список, содержащий все форты
	 */
	public GArray<Fortress> getFortressList()
	{
		return fortressList;
	}

	/**
	 * Возвращает список, содержащий все замки
	 */
	public GArray<Castle> getCastleList()
	{
		return castleList;
	}

	public SiegeUnit getResidenceByOwner(int ownerId, boolean onlyClanHall)
	{
		if(ownerId == 0)
			return null;
		for(SiegeUnit unit : unitsById.values())
			if(unit.getOwnerId() == ownerId && (onlyClanHall ? unit.isClanHall : true))
				return unit;

		return null;
	}

	/**
	 * Возвращает ближайший кланхол в радиусе если он есть иначе возвращает null
	 */
	public int findNearestClanHallIndex(int x, int y, int offset)
	{
		SiegeUnit unit = getClanHallByCoord(x, y);
		if(unit == null)
		{
			double closestDistance = offset;
			double distance;

			for(SiegeUnit unit2 : getClanHallList())
			{
				if(unit2 == null)
					continue;
				distance = unit2.getZone().getDistanceToZone(x, y);
				if(closestDistance > distance)
				{
					closestDistance = distance;
					unit = unit2;
				}
			}
		}
		if(unit != null)
			return unit.getId();
		else
			return -1;
	}

	/**
	 * Возвращает ближайший форт в радиусе если он есть иначе возвращает null
	 */
	public int findNearestFortressIndex(int x, int y, int offset)
	{
		SiegeUnit unit = getFortressByCoord(x, y);
		if(unit == null)
		{
			double closestDistance = offset;
			double distance;
			for(SiegeUnit unit2 : getFortressList())
			{
				if(unit2 == null)
					continue;
				distance = unit2.getZone().getDistanceToZone(x, y);
				if(closestDistance > distance)
				{
					closestDistance = distance;
					unit = unit2;
				}
			}
		}
		if(unit != null)
			return unit.getId();
		else
			return -1;
	}

	public void incrementZones()
	{
		for(SiegeUnit unit : unitsById.values())
		{
			for(L2Zone zone : zones.get(unit.getId()))
			{
				boolean added = false;
				if(zone.getTypes().contains(ZoneType.residence))
				{
					added = true;
					unit.setZone(zone);
					if(unit.getOwnerId() != 0)
						unit.getZone().setActive(true);
				}
				if(zone.getTypes().contains(ZoneType.siege))
				{
					added = true;
					unit.setSiegeZone(zone);
				}
				if(zone.getTypes().contains(ZoneType.headquarters))
				{
					added = true;
					unit.setHQZone(zone);
				}
				if(zone.getTypes().contains(ZoneType.siege_residence))
				{
					added = true;
					unit.setResidentZone(zone);
				}
				if(!added)
					unit.getTrapZones().put(zone.getZoneId(), zone);
			}
		}
	}

	private void load()
	{
		unitsById.clear();
		clanHallList.clear();
		fortressList.clear();
		castleList.clear();
		PreparedStatement statement = null;
		ResultSet rs = null;
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("select * from residence");

			rs = statement.executeQuery();

			while(rs.next())
			{
				String type = rs.getString("residenceType");
				Constructor<?> residence = null;
				SiegeUnit unit = null;
				ClanHall clanHall = null;
				Fortress fortress = null;
				Castle castle = null;
				try
				{
					residence = Class.forName("ru.l2gw.gameserver.model.entity." + type).getConstructors()[0];
				}
				catch(ClassNotFoundException e)
				{
					e.printStackTrace();
					_log.warn("cannot find residence class for " + rs.getInt("id"));
				}
				try
				{
					if(type.equals("ClanHall"))
						unit = (ClanHall) residence.newInstance();
					else if(type.equals("Fortress"))
						unit = (Fortress) residence.newInstance();
					else if(type.equals("Castle"))
						unit = (Castle) residence.newInstance();
					else
						continue;
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}

				unit.setId(rs.getInt("id"));
				unit.setName(rs.getString("name"));
				unit.setPrice(rs.getInt("price"));
				unit.setLease(rs.getInt("lease"));
				unit.setDesc(rs.getString("desc"));
				unit.setLocation(rs.getString("location"));
				unit.setGrade(rs.getInt("grade"));
				unit.setSiegeType(rs.getString("siegetype"));

				if(unit.isFort)
				{
					unit.setLastSiegeDate(rs.getLong("lastSiegeDate"));
					unit.loadContractCastle(rs.getInt("castleId"));
					unit.setLastTaxTime(rs.getLong("lastTax") * 1000);
					unit.loadSupplyLevel(rs.getInt("supplyLevel"));
					unit.loadRewardLevel(rs.getInt("rewardLevel"));
					unit.getSiege().setSiegeDateTime(rs.getLong("siegeDate") * 1000);
					fortress = (Fortress) unit;
					fortressList.add(fortress);
				}
				if(unit.isClanHall)
				{
					clanHall = (ClanHall) unit;
					clanHall.setLastPrice(rs.getLong("last_price"));
					clanHall.getPaidUntilCalendar().setTimeInMillis(rs.getLong("paidUntil"));
					clanHallList.add(clanHall);
				}
				if(unit.isCastle)
				{
					unit.getSiege().setSiegeDateTime(rs.getLong("siegeDate") * 1000);
					unit.getSiege().getChangeTimeEnd().setTimeInMillis(rs.getLong("changeTimeEnd") * 1000);
					unit.getSiege().setChangeTimeOver(rs.getBoolean("changeTimeOver"));
					unit.setLocation(unit.getName());
					unit.loadTown(rs.getInt("townId"));
					castle = (Castle) unit;
					castle.loadTaxPercent(rs.getInt("taxPercent"));
					castle.loadTreasury(rs.getLong("treasury"));

					castleList.add(castle);
				}
				unitsById.put(unit.getId(), unit);
			}
			_log.info("ResidenceManager: loaded " + unitsById.size() + " residences.");
		}
		catch(SQLException e)
		{
			System.out.println("Exception: ResidenceManager.load(): " + e.getMessage());
			e.printStackTrace();
		}

		try
		{
			statement = con.prepareStatement("Select clan_id,hasCastle,hasHideout,hasFortress from clan_data where hasCastle > 0 OR hasHideout > 0 OR hasFortress > 0");
			rs = statement.executeQuery();
			while(rs.next())
			{
				for(SiegeUnit unit : unitsById.values())
					if(unit.getId() == rs.getInt("hasCastle") || unit.getId() == rs.getInt("hasHideout") || unit.getId() == rs.getInt("hasFortress"))
					{
						unit.setOwnerId(rs.getInt("clan_id"));
						unit.loadFunctions();
						unit.startAutoTask();
					}
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception: ClanHall.load(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	private void loadDoor()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT id,unitId FROM siege_door ORDER by unitId");
			rset = statement.executeQuery();

			while(rset.next())
			{
				SiegeUnit unit = unitsById.get(rset.getInt("unitId"));
				L2DoorInstance door = DoorTable.getInstance().getDoor(rset.getInt("id"));
				unit.getDoors().add(door);
				door.setSiegeUnit(unit);
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception: loadDoor(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
}
