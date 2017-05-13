package ru.l2gw.gameserver.model.entity.vehicle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.L2VehicleAI;
import ru.l2gw.gameserver.model.entity.vehicle.actions.StationAction;
import ru.l2gw.gameserver.model.entity.vehicle.actions.destroyAction;
import ru.l2gw.util.Location;

import java.lang.reflect.Constructor;
import java.util.concurrent.ScheduledFuture;

/**
 * @author rage
 * @date 07.05.2010 9:49:59
 */
public class RouteStation implements Runnable
{
	private final int _stationId;
	private Location _point;
	private GArray<StationAction> _actions;
	private L2Vehicle _vehicle;
	private boolean _isDelayed;
	private int _currentAction = 0;
	private ScheduledFuture<?> _actionTask;

	private static Log _log = LogFactory.getLog("vehicle");

	public RouteStation(int stationId)
	{
		_stationId = stationId;
	}

	public int getStationId()
	{
		return _stationId;
	}

	public Location getPoint()
	{
		return _point;
	}

	public void setVehicle(L2Vehicle vehicle)
	{
		_vehicle = vehicle;
	}

	public boolean isDelayed()
	{
		return _isDelayed;
	}

	public void setDelayd(boolean delayed)
	{
		_isDelayed = delayed;
	}

	public void run()
	{
		try
		{
			_isDelayed = false;

			if(_currentAction >= _actions.size())
			{
				_currentAction = 0;
				((L2VehicleAI) _vehicle.getAI()).doTask();
				return;
			}

			StationAction sa = _actions.get(_currentAction);
			if(_vehicle instanceof L2ClanAirship)
				_log.info(_vehicle + " run: " + this + " exec: " + sa);
			sa.doAction(_vehicle);
			_currentAction++;
			if(!(sa instanceof destroyAction))
				_actionTask = ThreadPoolManager.getInstance().scheduleAi(this, sa.getDelay(), false);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public RouteStation copy()
	{
		RouteStation rs = new RouteStation(_stationId);
		rs._actions = _actions;
		rs._point = _point;
		return rs;
	}

	public void cancelTask()
	{
		if(_actionTask != null)
			_actionTask.cancel(true);
		_actionTask = null;
	}

	public static RouteStation parseStation(Node stationNode)
	{
		int id;
		RouteStation rs;
		try
		{
			id = Integer.parseInt(stationNode.getAttributes().getNamedItem("id").getNodeValue());
			rs = new RouteStation(id);

			for(Node n = stationNode.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if("point".equals(n.getNodeName()))
				{
					int x = Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue());
					int y = Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue());
					int z = Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue());
					int h = n.getAttributes().getNamedItem("h") != null ? Integer.parseInt(n.getAttributes().getNamedItem("h").getNodeValue()) : 0;
					rs._point = new Location(x, y, z, h);
				}
				else if("actions".equals(n.getNodeName()))
				{
					Class<?> clazz;
					Constructor<?> constructor;

					for(Node an = n.getFirstChild(); an != null; an = an.getNextSibling())
						if(an.getNodeType() == 1)
						{
							String action = an.getNodeName();
							clazz = Class.forName("ru.l2gw.gameserver.model.entity.vehicle.actions." + action + "Action");
							constructor = clazz.getConstructor();
							StationAction sa = (StationAction) constructor.newInstance();
							sa.parseAction(an);

							if(rs._actions == null)
								rs._actions = new GArray<StationAction>();

							rs._actions.add(sa);
						}
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("Cannot parse station in vehicledata.xml: " + e);
			e.printStackTrace();
			return null;
		}

		return rs;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "point: " + _point + " action: " + _actions.get(_currentAction);
	}
}
