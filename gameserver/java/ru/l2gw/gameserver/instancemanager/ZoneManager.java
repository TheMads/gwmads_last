package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class ZoneManager
{
	private static final Log _log = LogFactory.getLog(ZoneManager.class.getName());
	private static ZoneManager _instance;
	private Map<ZoneType, List<L2Zone>> zonesByType;
	private static L2Zone[][][] zones;

	public static ZoneManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new ZoneManager();
			_instance.load();
		}
		return _instance;
	}

	private void load()
	{
		int zoneCount = 0;
		zones = new L2Zone[L2World.WORLD_SIZE_X][L2World.WORLD_SIZE_Y][];

		List<File> _zoneFiles = new LinkedList<>();
		try
		{
			File dir = new File("data/zones");

			if(!dir.exists())
			{
				_log.warn("Dir " + dir.getAbsolutePath() + " not exists");
				return;
			}

			File[] files = dir.listFiles();
			if(files != null)
				for(File f : files)
				{
					if(f.getName().endsWith(".xml"))
						_zoneFiles.add(f);
				}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to load zone files.");
		}

		zonesByType = new HashMap<>();
		// Load the zone xml
		try
		{
			for(File _file : _zoneFiles)
			{
				_log.info("ZoneManager: load file " + _file.getName());

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);

				if(!_file.exists())
				{
					if(Config.DEBUG)
						_log.info("The " + _file.getName() + " file is missing.");
					continue;
				}

				Document doc = factory.newDocumentBuilder().parse(_file);

				for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if("list".equalsIgnoreCase(n.getNodeName()))
					{
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if("zone".equalsIgnoreCase(d.getNodeName()))
							{
								L2Zone zone = L2Zone.parseZone(d);
								if(zone == null) continue;
								int ax, ay, bx, by;

								for(int x = 0; x < zones.length; x++)
								{
									for(int y = 0; y < zones[x].length; y++)
									{
										ax = Config.GEO_X_FIRST + x - 20 << 15;
										ay = Config.GEO_Y_FIRST + y - 18 << 15;
										bx = ax + 32767;
										by = ay + 32767;
										if(zone.intersectsRectangle(ax, bx, ay, by))
										{
											if(zones[x][y] == null)
												zones[x][y] = new L2Zone[]{zone};
											else
											{
												L2Zone[] za = new L2Zone[zones[x][y].length + 1];
												System.arraycopy(zones[x][y], 0, za, 0, zones[x][y].length);
												za[za.length - 1] = zone;
												zones[x][y] = za;
											}
										}
									}
								}

								for(ZoneType zt : zone.getTypes())
								{
									if(!zonesByType.containsKey(zt))
										zonesByType.put(zt, new LinkedList<L2Zone>());
									zonesByType.get(zt).add(zone);
								}
								zoneCount++;
							}
						}
					}
				}
			}

			for(L2Zone[][] zoneXY : zones)
				for(L2Zone[] zoneList : zoneXY)
					if(zoneList != null)
						for(L2Zone zone : zoneList)
							zone.register();

			_log.info("ZoneManager: loaded zones: " + zoneCount);
		}
		catch(Exception e)
		{
			_log.error("Error while loading zones.", e);
			return;
		}
	}

	public void clearAllZones()
	{
		TownManager.getInstance().clearAllZones();
		ResidenceManager.getInstance().clearAllZones();
		DimensionalRiftManager.getInstance().clearAllZones();
		zonesByType = null;
		zones = null;
	}

	public void reloadZones()
	{
		clearAllZones();
		load();
		DimensionalRiftManager.getInstance().init();
		ResidenceManager.getInstance().incrementZones();
	}

	public List<L2Zone> getZones(ZoneType type)
	{
		if(!zonesByType.containsKey(type))
			return Collections.emptyList();

		return zonesByType.get(type);
	}

	public final L2Zone isInsideZone(ZoneType zt, int x, int y)
	{
		for(L2Zone temp : getZones(zt))
			if(temp.isActive() && temp.isInsideZone(x, y))
				return temp;

		return null;
	}

	public final L2Zone isInsideZone(ZoneType zt, int x, int y, int z)
	{
		for(L2Zone temp : getZones(zt))
			if(temp.isActive() && temp.isInsideZone(x, y, z))
				return temp;

		return null;
	}

	public final L2Zone getZoneById(ZoneType zt, int zoneId)
	{
		for(L2Zone zone : getZones(zt))
		{
			if(zone.getZoneId() == zoneId)
				return zone;
		}
		return null;
	}

	public final L2Zone getZoneByName(String name)
	{
		if(name == null || name.isEmpty())
			return null;

		for(ZoneType zt : zonesByType.keySet())
			for(L2Zone zone : zonesByType.get(zt))
				if(name.equalsIgnoreCase(zone.getZoneName()))
					return zone;

		return null;
	}

	public void areaSetOnOff(String name, int on)
	{
		L2Zone zone = getZoneByName(name);
		if(zone != null)
			zone.setActive(on == 1);
	}

	public void areaSetOnOff(String name, int on, int reflection)
	{
		L2Zone zone = getZoneByName(name);
		if(zone != null)
			zone.setActive(on == 1, reflection);
	}

	public L2Zone[] getAllZones(int x, int y)
	{
		int gx = (x - L2World.MAP_MIN_X) >> 15;
		int gy = (y - L2World.MAP_MIN_Y) >> 15;
		if(gx < 0 || gx >= zones.length || gy < 0 || gy >= zones[gx].length)
		{
			_log.warn("Wrong world region: " + gx + " " + gy + " (" + x + "," + y + ")");
			return null;
		}

		return zones[gx][gy];
	}

	public GArray<L2Zone> getZones(int x, int y, int z)
	{
		L2Zone[] za = getAllZones(x, y);
		if(za == null)
			return null;

		GArray<L2Zone> res = new GArray<>(za.length);
		for(L2Zone zone : za)
			if(zone.isInsideZone(x, y, z))
				res.add(zone);

		return res.isEmpty() ? null : res;
	}
}