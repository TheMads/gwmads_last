package ru.l2gw.gameserver.model.entity.siege.reinforce;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.serverpackets.EventTrigger;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: admin
 * Date: 13.02.2009
 * Time: 14:38:58
 */
public class TrapReinforce extends Reinforce
{
	private Map<Integer, FastList<Integer>> _zones;
	private int _eventId;

	public TrapReinforce(int id, int level, int siegeUnitId)
	{
		super(id, level, siegeUnitId);
		_zones = new FastMap<Integer, FastList<Integer>>();
	}

	public void addZone(int level, int zoneId)
	{
		if (_zones.get(level) != null)
			_zones.get(level).add(zoneId);
		else
		{
			FastList<Integer> z = new FastList<Integer>();
			z.add(zoneId);
			_zones.put(level, z);
		}
	}

	public int getEventId()
	{
		return _eventId;
	}

	public void setEventId(int eventId)
	{
		_eventId = eventId;
	}

	@Override
	public String getType()
	{
		return "TRAP";
	}

	public void setActive(boolean active)
	{
		_active = active;
		for(Integer level : _zones.keySet())
			if(level <= _level || !active)
			{
				for(Integer zoneId : _zones.get(level))
				{
					ResidenceManager.getInstance().getBuildingById(_siegeUnitId).getTrapZones().get(zoneId).setActive(active);
					ResidenceManager.getInstance().getBuildingById(_siegeUnitId).getSiegeZone().broadcastPacket(new EventTrigger(_eventId, active));
				}
			}
	}
}
