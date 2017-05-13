package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.handler.ScriptHandler;
import ru.l2gw.gameserver.instancemanager.*;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.base.Race;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.util.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class MapRegionTable
{
	private final static Log _log = LogFactory.getLog(MapRegionTable.class.getName());

	private static MapRegionTable _instance;

	private final int[][] _regions = new int[L2World.WORLD_SIZE_X][L2World.WORLD_SIZE_Y];

	public static enum TeleportWhereType
	{
		Castle,
		ClanHall,
		ClosestTown,
		SecondClosestTown,
		Headquarter,
		Fortress
	}

	public static MapRegionTable getInstance()
	{
		if(_instance == null)
			_instance = new MapRegionTable();
		return _instance;
	}

	private MapRegionTable()
	{
		int count = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM mapregion");
			rset = statement.executeQuery();
			while(rset.next())
			{
				int y = rset.getInt("y10_plus");

				for(int i = Config.GEO_X_FIRST; i <= Config.GEO_X_LAST; i++)
				{
					int region = rset.getInt("x" + i);
					_regions[i - Config.GEO_X_FIRST][y] = region;
					count++;
				}
			}

			_log.info("Loaded " + count + " mapregions.");
		}
		catch(Exception e)
		{
			_log.warn("error while creating map region data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public final int getMapRegion(int posX, int posY)
	{
		int tileX = posX - L2World.MAP_MIN_X >> 15;
		int tileY = posY - L2World.MAP_MIN_Y >> 15;
		return _regions[tileX][tileY];
	}

	public Location getTeleToLocation(L2Character cha, TeleportWhereType teleportWhere)
	{
		L2Player player = cha.getPlayer();

		if(player != null)
		{
			Location loc = ScriptHandler.getInstance().onEscape(player);
			if(loc != null)
				return loc;

			if(player.getReflection() != 0)
			{
				Instance inst = InstanceManager.getInstance().getInstanceByReflection(player.getReflection());
				if(inst != null && inst.getEndPos() != null && player.getStablePoint() == null)
					return inst.getEndPos();
			}

			if(player.isInJail())
				return new Location(-114648, -249384, -2984);

			L2Clan clan = player.getClan();

			if(clan != null)
			{
				// If teleport to clan hall
				if(teleportWhere == TeleportWhereType.ClanHall && clan.getHasUnit(1))
				{
					SiegeUnit unit = ResidenceManager.getInstance().getResidenceByOwner(clan.getClanId(), true);
					if(unit != null && (unit.getSiege() == null || !unit.getSiege().isInProgress()))
					{
						player.setStablePoint(null);
						return unit.getZone().getSpawn(player);
					}
					else if(unit != null && unit.getSiege() != null && unit.getSiege().isInProgress())
					{
						player.setStablePoint(null);
						return unit.getRezidentZone().getSpawn(player);
					}
				}
				// If teleport to castle
				if(teleportWhere == TeleportWhereType.Castle)
				{
					SiegeUnit unit;
					if(clan.getHasUnit(2))
						unit = ResidenceManager.getInstance().getBuildingById(clan.getHasCastle());
					else
						unit = SiegeManager.getCastleDefenderSiegeUnit(clan.getClanId());

					if(unit != null)
					{
						player.setStablePoint(null);
						return unit.getRezidentZone().getSpawn(player);
					}
				}
				// If teleport to fortress
				if(teleportWhere == TeleportWhereType.Fortress && clan.getHasUnit(3))
				{
					SiegeUnit unit = ResidenceManager.getInstance().getBuildingById(clan.getHasFortress());
					if(!unit.getSiege().isInProgress())
					{
						player.setStablePoint(null);
						return unit.getZone().getSpawn(player);
					}
					else if(unit.getOwnerId() == player.getClanId())
					{
						player.setStablePoint(null);
						return unit.getRezidentZone().getSpawn(player);
					}
				}
				// Checking if in siege
				Siege siege = SiegeManager.getSiege(player);
				if(siege != null && siege.isInProgress())
				{
					if(teleportWhere == TeleportWhereType.Headquarter)
					{
						// Check if player's clan is attacker
						L2NpcInstance flag = siege.getHeadquarter(player.getClanId());
						if(flag != null)
							// Спаун рядом с флагом
							return Location.coordsRandomize(flag.getLoc(), 49, 51);
					}

					if(siege.getSiegeUnit().isCastle &&
							SevenSigns.getInstance().getPlayerCabal(player) != SevenSigns.CABAL_NULL &&
							SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL &&
							SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.getInstance().getPlayerCabal(player))
						return TownManager.getInstance().getSecondBuildingByObject(player).getSpawn(player);

					return TownManager.getInstance().getBuildingByObject(player).getSpawn(player);
				}
			}

			// Светлые эльфы не могут воскрешаться в городе темных
			if(player.getRace() == Race.elf && TownManager.getInstance().getBuildingByObject(player).getTownId() == 3)
				return TownManager.getInstance().getBuildingById(2).getSpawn(player);

			// Темные эльфы не могут воскрешаться в городе светлых
			if(player.getRace() == Race.darkelf && TownManager.getInstance().getBuildingByObject(player).getTownId() == 2)
				return TownManager.getInstance().getBuildingById(3).getSpawn(player);

			GArray<L2Zone> zones = player.getZones();
			if(zones != null)
				for(L2Zone zone : zones)
					if(zone != null && zone.getRestartPoints() != null && !zone.getTypes().contains(L2Zone.ZoneType.residence) && !zone.getTypes().contains(L2Zone.ZoneType.siege) && !zone.getTypes().contains(L2Zone.ZoneType.siege_residence) && !zone.getTypes().contains(L2Zone.ZoneType.olympiad_stadia))
						return zone.getSpawn(player);

			loc = FieldCycleManager.getRestartPoint(player);
			if(loc != null)
				return loc;

			return TownManager.getInstance().getBuildingByObject(player).getSpawn(player);
		}

		return TownManager.getInstance().getBuildingByObject(player).getSpawn(player);
	}

	public static Location getTeleToCastle(L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.Castle);
	}

	public static Location getTeleToClanHall(L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.ClanHall);
	}

	public static Location getTeleToClosestTown(L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.ClosestTown);
	}

	public static Location getTeleToSecondClosestTown(L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.SecondClosestTown);
	}

	public static Location getTeleToHeadquarter(L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.Headquarter);
	}

	public static Location getTeleToFortress(L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.Fortress);
	}

	public static Location getTeleTo(L2Character activeChar, TeleportWhereType teleportWhere)
	{
		return getInstance().getTeleToLocation(activeChar, teleportWhere);
	}
}