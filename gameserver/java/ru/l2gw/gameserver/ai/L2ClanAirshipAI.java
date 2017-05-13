package ru.l2gw.gameserver.ai;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;

import java.util.concurrent.ScheduledFuture;

/**
 * @author rage
 * @date 08.09.2010 16:54:40
 */
public class L2ClanAirshipAI extends L2VehicleAI
{
	private ScheduledFuture<?> _task;
	private L2ClanAirship _clanAirship;
	private long _controlTime = 0;

	public L2ClanAirshipAI(L2Character actor)
	{
		super(actor);
		_clanAirship = (L2ClanAirship) actor;
	}

	public void depart()
	{
		if(_currentStation != null)
			_currentStation.cancelTask();

		_routeStations = _vehicle.getRouteStations();
		_currentStationIndex = 0;
		_currentStation = null;
		doTask();
	}

	@Override
	public void doTask()
	{
		if(_vehicle.isManualControlled() || _task == null)
			return;

		_log.info("doTask: dock: " + _clanAirship.getCurrentDock() + " station index: " + _currentStationIndex + " routes: " + _vehicle.getRouteStations().size());

		super.doTask();
	}

	@Override
	public void startAITask()
	{
		if(_task != null)
			_task.cancel(true);

		_task = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ClanAirshipTask(), 10000, 60000);
	}

	public void stopAITask()
	{
		if(_task != null)
			_task.cancel(false);

		_task = null;
	}

	private class ClanAirshipTask implements Runnable
	{
		public void run()
		{
			_log.info("L2ClanAirshipAI: run task. Controlled: " + _clanAirship.isManualControlled());
			if(_clanAirship.isManualControlled())
			{
				_controlTime = 0;
				_clanAirship.updateOnBoardPlayers();
				if(_clanAirship.getOnBoardPlayer() == null || _clanAirship.getOnBoardPlayer().size() < 1)
				{
					_log.info("L2ClanAirshipAI: no players on board, delete airship.");
					_clanAirship.deleteMe();
					return;
				}

				_clanAirship.setCurrentEp(_clanAirship.getCurrentEp() - 10);
				_clanAirship.broadcastUserInfo();
			}
			else if(_controlTime == 0)
				_controlTime = System.currentTimeMillis();
			else if(_controlTime + 1200000 < System.currentTimeMillis())
			{
				_clanAirship.updateOnBoardPlayers();
				if(_clanAirship.getOnBoardPlayer() == null || _clanAirship.getOnBoardPlayer().size() < 1)
				{
					_log.info("L2ClanAirshipAI: no players on board, delete airship.");
					_clanAirship.deleteMe();
				}
			}
		}
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}
}
