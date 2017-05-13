package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Reflection
{
	private int _id;
	private Map<Integer, L2Object> _objects = new ConcurrentHashMap<>();
	private static Log _log = LogFactory.getLog("instances");
	private GCSArray<L2Zone> _zones;

	public Reflection(int id)
	{
		_id = id;
	}

	public void collapse()
	{
		if(Config.DEBUG_INSTANCES)
			_log.info(this + ": collapse");
		for(L2Object o : _objects.values())
		{
			if(o != null)
			{
				if(o instanceof L2NpcInstance)
				{
					if(Config.DEBUG_INSTANCES)
						_log.info(this + ": collapse object " + o + " deleteMe()");
					o.deleteMe();
				}
				else if(o instanceof L2ItemInstance && ((L2ItemInstance) o).getOwnerId() > 0)
				{
				}
				else
				{
					if(Config.DEBUG_INSTANCES)
						_log.info(this + ": collapse object " + o + " decayMe");
					o.decayMe();

					if(!o.isPlayable())
					{
						L2World.removeObject(o);
						L2ObjectsStorage.remove(o.getStoredId());
					}
					else if(o.isPlayer())
						o.getPlayer().teleToClosestTown();
				}
			}
		}
		_objects.clear();

		if(_zones != null)
			for(L2Zone zone : _zones)
				zone.onReflectionCollapse(_id);
	}

	public int getId()
	{
		return _id;
	}

	public void addObject(L2Object o)
	{
		if(Config.DEBUG_INSTANCES)
			_log.info(this + ": add object: " + o + " objId: " + o._objectId);
		_objects.put(o._objectId, o);
		if(_zones != null && _zones.size() > 0 && o instanceof L2Character)
			o.revalidateZones(true);
	}

	public void removeObject(int i)
	{
		if(Config.DEBUG_INSTANCES)
			_log.info(this + ": remove object id: " + i);
		L2Object obj = _objects.remove(i);
		if(_zones != null && _zones.size() > 0 && obj instanceof L2Character)
			obj.revalidateZones(true);
	}

	public Collection<L2Object> getAllObjects()
	{
		return _objects.values();
	}

	public void addZone(L2Zone zone)
	{
		if(_zones == null)
			_zones = new GCSArray<L2Zone>(1);

		if(!_zones.contains(zone))
			_zones.add(zone);
	}

	@Override
	public String toString()
	{
		return "Reflection[refId=" + _id + ";]";
	}
}
