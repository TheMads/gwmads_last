package ru.l2gw.gameserver.model.entity.vehicle;

import javolution.util.FastMap;
import ru.l2gw.extensions.listeners.L2ZoneEnterLeaveListener;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.VehicleManager;
import ru.l2gw.gameserver.instancemanager.ZoneManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.templates.L2CharTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 07.09.2010 16:07:49
 */
public class L2AirShipDock
{
	private final FastMap<String, GArray<RouteStation>> _stations;
	private final GArray<StatsSet> _teleports;
	private final String _name;
	private final int _dockId;
	private int _clientDockId;
	private L2ClanAirship _dockedShip = null;

	public L2AirShipDock(int id, String name)
	{
		_dockId = id;
		_name = name;
		_stations = new FastMap<String, GArray<RouteStation>>();
		_teleports = new GArray<StatsSet>(3);
	}

	public void addTeleport(StatsSet tp)
	{
		_teleports.add(tp);
	}

	public GArray<StatsSet> getTeleports()
	{
		return _teleports;
	}

	public void addRouteStation(String type, RouteStation rs)
	{
		GArray<RouteStation> route = _stations.get(type);
		if(route == null)
		{
			route = new GArray<RouteStation>(4);
			_stations.put(type, route);
		}

		route.add(rs);
	}

	public void summonClanAirShip(L2Clan owner)
	{
		if(_dockedShip != null)
			return;

		_dockedShip = new L2ClanAirship(IdFactory.getInstance().getNextId(), new L2CharTemplate(VehicleManager.getEmptyStatsSet()));
		_dockedShip.setClan(owner);
		_dockedShip.setCurrentDock(this);
		_dockedShip.setCurrentRoute(_stations.get("summon"));
		VehicleManager.getInstance().addVehicle(_dockedShip);
		_dockedShip.spawnMe();
	}

	public L2ClanAirship getDockedShip()
	{
		return _dockedShip;
	}

	public StatsSet getPort(int portId)
	{
		for(StatsSet port : _teleports)
			if(port.getInteger("port_id") == portId)
				return port;

		return null;
	}

	public void setDockedShip(L2ClanAirship ship)
	{
		_dockedShip = ship;
	}

	public void setClientDockId(int docId)
	{
		_clientDockId = docId;
	}

	public int getClientDockId()
	{
		return _clientDockId;
	}

	public GArray<RouteStation> getRouteByType(String type)
	{
		return _stations.get(type);
	}

	public void setDockZone(int zoneId)
	{
		L2Zone zone = ZoneManager.getInstance().getZoneById(L2Zone.ZoneType.dummy, zoneId);
		if(zone != null)
			zone.getListenerEngine().addMethodInvokedListener(new LandZoneListener());
		else
			System.out.println(this + " no landing zone: " + zoneId);
	}

	@Override
	public String toString()
	{
		return "L2AirshipDock[id=" + _dockId +";name=" + _name + "]";	
	}

	private class LandZoneListener extends L2ZoneEnterLeaveListener
	{
		@Override
		public void objectEntered(L2Zone zone, L2Character object)
		{
			if(object instanceof L2ClanAirship && ((L2ClanAirship) object).isManualControlled())
			{
				L2ClanAirship clanAirship = (L2ClanAirship) object;
				clanAirship.stopMove();
				clanAirship.setManualControl(false);
				clanAirship.setCurrentRoute(getRouteByType("docking"));
				clanAirship.getAI().depart();
			}
		}

		@Override
		public void objectLeaved(L2Zone zone, L2Character object)
		{
		}

		@Override
		public void sendZoneStatus(L2Zone zone, L2Player object)
		{
		}
	}
}
