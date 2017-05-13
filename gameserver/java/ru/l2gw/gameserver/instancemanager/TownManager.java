package ru.l2gw.gameserver.instancemanager;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.entity.Town;
import ru.l2gw.gameserver.model.zone.L2TownZone;
import ru.l2gw.gameserver.tables.MapRegionTable;
import ru.l2gw.commons.arrays.GArray;

public final class TownManager
{
	private static TownManager _instance;
	private GArray<Town> _towns;

	public static TownManager getInstance()
	{
		if(_instance == null)
			_instance = new TownManager();
		return _instance;
	}

	public void addZone(L2TownZone zone)
	{
		if(_towns == null)
			_towns = new GArray<Town>();
		_towns.add(new Town(zone));
	}

	public void clearAllZones()
	{
		_towns = null;
	}

	public Town getBuildingByCoord(int x, int y)
	{
		return getBuildingById(MapRegionTable.getInstance().getMapRegion(x, y));
	}

	public Town getBuildingByObject(L2Object activeObject)
	{
		return getBuildingById(MapRegionTable.getInstance().getMapRegion(activeObject.getX(), activeObject.getY()));
	}

	public Town getSecondBuildingByObject(L2Object activeObject)
	{
		int[] secondtown = { 0, 1, 2, 3, 4, 5, 7, 7, 9, 13, 12, 15, 12, 9, 15, 14, 11, 17, 18, 19 };
		return getBuildingById(secondtown[MapRegionTable.getInstance().getMapRegion(activeObject.getX(), activeObject.getY())]);
	}

	public int getClosestTownNumber(L2Character cha)
	{
		return MapRegionTable.getInstance().getMapRegion(cha.getX(), cha.getY());
	}

	public String getClosestTownName(L2Character cha)
	{
		return getBuildingByObject(cha).getName();
	}

	public Town getBuildingById(int townId)
	{
		for(Town town : _towns)
			if(town.getTownId() == townId)
				return town;

		return null;
	}

	public GArray<Town> getTowns()
	{
		return _towns;
	}
}