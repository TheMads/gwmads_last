package ru.l2gw.gameserver.model.entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TownManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.zone.L2TownZone;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.util.Location;

public class Town
{
	protected static Log _log = LogFactory.getLog(Town.class.getName());

	// =========================================================
	// Data Field
	//private double _TaxRate = 0;    // This is the town's local tax rate used by merchant
	private int _townId = 0;
	private L2TownZone _zone;

	// =========================================================
	// Constructor
	public Town(L2TownZone zone)
	{
		_townId = zone.getEntityId();
		_zone = zone;
//		loadData();
	}

	// =========================================================
	// Method - Public
	/** Return true if object is inside the zone */
	public boolean checkIfInZone(L2Object obj)
	{
		return checkIfInZone(obj.getX(), obj.getY());
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(int x, int y)
	{
		return _zone.isInsideZone(x, y);
	}

	// =========================================================
	// Property
	public final Castle getCastle()
	{
		return ResidenceManager.getInstance().getCastleById(getCastleId());
	}

	public final SiegeUnit getCastleLikeUnit()
	{
		return ResidenceManager.getInstance().getBuildingById(getCastleId());
	}

	public final int getCastleId()
	{
		return _zone != null ? _zone.getTaxById() : 0;
	}

	public final String getName()
	{
		return _zone != null ? _zone.getZoneName() : "unknown town";
	}

	public final Location getSpawn(L2Character cha)
	{
		// Если печатью владеют лорды Рассвета (Dawn), и в данном городе идет осада, то телепортирует во 2-й по счету город.
		if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN && _zone.getRedirectTownId() != getTownId() && getCastle() != null && getCastle().getSiege().isInProgress())
			return TownManager.getInstance().getBuildingById(_zone.getRedirectTownId()).getZone().getSpawn(cha);
		return _zone.getSpawn(cha);
	}

	public final int getRedirectToTownId()
	{
		return _zone.getRedirectTownId();
	}

	public final int getTownId()
	{
		return _townId;
	}

	public final L2Zone getZone()
	{
		return _zone;
	}
}
