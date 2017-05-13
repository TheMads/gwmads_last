package ru.l2gw.gameserver.model.entity.vehicle;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.ai.L2ClanAirshipAI;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.VehicleManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExAirShipInfo;
import ru.l2gw.gameserver.templates.L2CharTemplate;

/**
 * @author rage
 * @date 08.09.2010 17:03:08
 */
public class L2ClanAirship extends L2AirShip
{
	private L2Clan _clanOwner;
	private L2AirShipDock _currentDock;
	private final L2AirShipHelm _helm;
	private int _currentEp;
	private int _captainObjectId;
	private boolean _manualControl = false;

	public L2ClanAirship(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
		_helm = new L2AirShipHelm(IdFactory.getInstance().getNextId(), this);
	}

	@Override
	public int getHelmId()
	{
		return _helm.getObjectId();
	}

	@Override
	public L2AirShipHelm getHelm()
	{
		return _helm;
	}

	public void setCurrentEp(int ep)
	{
		_currentEp = Math.min(Math.max(ep, 0), 600);
	}

	public int getCurrentEp()
	{
		return _currentEp;
	}

	public void setClan(L2Clan clan)
	{
		_clanOwner = clan;
		if(clan != null)
			_clanOwner.setAirship(this);
	}

	public void setCurrentDock(L2AirShipDock dock)
	{
		_currentDock = dock;
	}

	public L2AirShipDock getCurrentDock()
	{
		return _currentDock;
	}

	public void setManualControl(boolean manual)
	{
		_manualControl = manual;
	}

	public boolean isManualControlled()
	{
		return _manualControl;
	}
	
	@Override
	public float getMoveSpeed()
	{
		return _currentEp > 0 || !_manualControl ? 280 : 70;
	}

	public int getRotationSpeed()
	{
		return 2000;
	}

	public void setCurrentRoute(GArray<RouteStation> route)
	{
		_routeStations = new GArray<RouteStation>(route.size());
		for(RouteStation r : route)
		{
			RouteStation rs = r.copy();
			rs.setVehicle(this);
			_routeStations.add(rs);
		}
	}

	public void setCaptainObjectId(int objectId)
	{
		_captainObjectId = objectId;
	}

	public int getCaptainObjectId()
	{
		return _captainObjectId;
	}

	public void deleteMe()
	{
		if(getOnBoardPlayer() != null)
			for(int objectId : getOnBoardPlayer())
			{
				L2Player player = L2ObjectsStorage.getPlayer(objectId);
				if(player != null && player.getVehicle() == this)
					oustPlayer(player, getKickPoint());
			}

		VehicleManager.getInstance().removeVehicle(getObjectId());
		super.deleteMe();

		if(_clanOwner != null)
			_clanOwner.setAirship(null);
		_clanOwner = null;

		if(_currentDock != null)
			_currentDock.setDockedShip(null);
		_currentDock = null;

		detachAI();
	}

	public void broadcastUserInfo()
	{
		broadcastPacket(new ExAirShipInfo(this));
	}

	@Override
	public L2ClanAirshipAI getAI()
	{
		if(_ai == null)
		{
			_ai = new L2ClanAirshipAI(this);
			_ai.startAITask();
		}
		
		return (L2ClanAirshipAI) _ai;
	}

	@Override
	public synchronized void removePlayerFromBoard(L2Player player)
	{
		super.removePlayerFromBoard(player);
		if(_captainObjectId == player.getObjectId())
		{
			_captainObjectId = 0;
			broadcastUserInfo();
		}
	}
}
