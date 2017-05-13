package ru.l2gw.gameserver.ai;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.model.entity.vehicle.RouteStation;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 07.05.2010 12:20:22
 */
public class L2VehicleAI extends L2CharacterAI
{
	protected L2Vehicle _vehicle;
	protected int _currentStationIndex;
	protected GArray<RouteStation> _routeStations;
	protected RouteStation _currentStation;

	public L2VehicleAI(L2Character actor)
	{
		super(actor);
		_vehicle = (L2Vehicle) actor;
		_routeStations = _vehicle.getRouteStations();
		_currentStationIndex = 0;
		_currentStation = null;
	}

	public void doTask()
	{
		if(_vehicle.isMoving)
			return;

		if(_routeStations == null || _routeStations.size() < 2)
			return;

		if(_currentStationIndex >= _routeStations.size())
			_currentStationIndex = 0;

		_currentStation = _routeStations.get(_currentStationIndex);
		ThreadPoolManager.getInstance().executeAi(_currentStation, false);
	}

	public int getCurrentStationIndex()
	{
		return _currentStationIndex;
	}

	@Override
	protected void onEvtArrived()
	{
		if(_currentStation.isDelayed())
			return;

		_currentStationIndex++;
		doTask();
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}
}
