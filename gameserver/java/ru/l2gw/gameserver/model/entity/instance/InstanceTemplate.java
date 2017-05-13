package ru.l2gw.gameserver.model.entity.instance;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.crontab.Crontab;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.XmlUtil;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.npcmaker.DefaultMaker;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.util.Location;

import java.util.List;
import java.util.Map;

/**
 * @author: rage
 * @date: 23.07.2009 17:50:02
 */
public class InstanceTemplate
{
	private int _id;
	private int _minParty;
	private int _maxParty;
	private int _minLevel;
	private int _maxLevel;
	private int _maxCount = -1;
	private int _type;
	private long _timeLimit;
	private long _coolTime = 300000;
	private long _noUserTimeout = 0;
	private String _name;
	private int _startPosType = 0;
	private GArray<Location> _startLoc;
	private GArray<Location> _endLoc;
	private List<InstanceSpawn> _spawns;
	private Map<Integer, Boolean> _doors;
	private L2Zone _zone;
	private String _className;
	private boolean _dispel = true;
	private Crontab _reuseReset;
	private Crontab _premiumReuse;
	private GArray<Location> _restartPoints;
	private Map<String, DefaultMaker> _makers;
	private int _checkQuest;
	private GArray<String> _areaList;
	private GArray<Integer> dispelOnExit;

	public InstanceTemplate(int id, String name, int type, String reuseReset, String premiumReuse)
	{
		_id = id;
		_name = name;
		_type = type;
		_spawns = new FastList<>();
		_doors = new FastMap<>();
		_reuseReset = new Crontab(reuseReset);
		_premiumReuse = new Crontab(premiumReuse);
	}

	public static InstanceTemplate parseInstance(Node i)
	{
		int id = Integer.parseInt(i.getAttributes().getNamedItem("id").getNodeValue());
		String name = i.getAttributes().getNamedItem("name").getNodeValue();
		int type = Integer.parseInt(i.getAttributes().getNamedItem("type").getNodeValue());
		String reuseReset = i.getAttributes().getNamedItem("reuseReset") != null ? i.getAttributes().getNamedItem("reuseReset").getNodeValue() : "30 6 * * *";
		String premiumReuse = i.getAttributes().getNamedItem("premiumReuse") != null ? i.getAttributes().getNamedItem("premiumReuse").getNodeValue() : reuseReset;

		InstanceTemplate it = new InstanceTemplate(id, name, type, reuseReset, premiumReuse);
		it.setClassName(i.getAttributes().getNamedItem("class") != null ? i.getAttributes().getNamedItem("class").getNodeValue() : null);
		it._dispel = i.getAttributes().getNamedItem("dispel") == null || Boolean.parseBoolean(i.getAttributes().getNamedItem("dispel").getNodeValue());

		Node maxCount = i.getAttributes().getNamedItem("max");
		if(maxCount != null)
			it.setMaxCount(Integer.parseInt(maxCount.getNodeValue()));

		for(Node n = i.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if("party".equalsIgnoreCase(n.getNodeName()))
			{
				it.setMinParty(n.getAttributes().getNamedItem("min") != null ? Integer.parseInt(n.getAttributes().getNamedItem("min").getNodeValue()) : 2);
				it.setMaxParty(n.getAttributes().getNamedItem("max") != null ? Integer.parseInt(n.getAttributes().getNamedItem("max").getNodeValue()) : 2);
			}
			else if("level".equalsIgnoreCase(n.getNodeName()))
			{
				it.setMinLevel(n.getAttributes().getNamedItem("min") != null ? Integer.parseInt(n.getAttributes().getNamedItem("min").getNodeValue()) : 1);
				it.setMaxLevel(n.getAttributes().getNamedItem("max") != null ? Integer.parseInt(n.getAttributes().getNamedItem("max").getNodeValue()) : 85);
			}
			else if("checkQuest".equalsIgnoreCase(n.getNodeName()))
			{
				it._checkQuest = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
			}
			else if("timelimit".equalsIgnoreCase(n.getNodeName()))
			{
				int h = n.getAttributes().getNamedItem("hour") != null ? Integer.parseInt(n.getAttributes().getNamedItem("hour").getNodeValue()) : 0;
				int m = n.getAttributes().getNamedItem("min") != null ? Integer.parseInt(n.getAttributes().getNamedItem("min").getNodeValue()) : 0;
				int s = n.getAttributes().getNamedItem("sec") != null ? Integer.parseInt(n.getAttributes().getNamedItem("sec").getNodeValue()) : 0;

				it.setTimeLimit(h * 60 * 60000 + m * 60000 + s * 1000);
			}
			else if("cooltime".equalsIgnoreCase(n.getNodeName()))
			{
				int h = n.getAttributes().getNamedItem("hour") != null ? Integer.parseInt(n.getAttributes().getNamedItem("hour").getNodeValue()) : 0;
				int m = n.getAttributes().getNamedItem("min") != null ? Integer.parseInt(n.getAttributes().getNamedItem("min").getNodeValue()) : 0;
				int s = n.getAttributes().getNamedItem("sec") != null ? Integer.parseInt(n.getAttributes().getNamedItem("sec").getNodeValue()) : 0;

				it._coolTime = h * 60L * 60000 + m * 60000 + s * 1000;
			}
			else if("noUserTimeout".equalsIgnoreCase(n.getNodeName()))
			{
				int h = n.getAttributes().getNamedItem("hour") != null ? Integer.parseInt(n.getAttributes().getNamedItem("hour").getNodeValue()) : 0;
				int m = n.getAttributes().getNamedItem("min") != null ? Integer.parseInt(n.getAttributes().getNamedItem("min").getNodeValue()) : 0;
				int s = n.getAttributes().getNamedItem("sec") != null ? Integer.parseInt(n.getAttributes().getNamedItem("sec").getNodeValue()) : 0;

				it._noUserTimeout = h * 60L * 60000 + m * 60000 + s * 1000;
			}
			else if("startPos".equalsIgnoreCase(n.getNodeName()))
			{
				it._startPosType = n.getAttributes().getNamedItem("type") == null ? 0 : "random".equalsIgnoreCase(n.getAttributes().getNamedItem("type").getNodeValue()) ? 1 : 0;
			}
			else if("startLoc".equalsIgnoreCase(n.getNodeName()))
				it.addStartLoc(new Location(Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue()), Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue()), Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue())));
			else if("endLoc".equalsIgnoreCase(n.getNodeName()))
				it.addEndLoc(new Location(Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue()), Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue()), Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue())));
			else if("dispelOnExit".equalsIgnoreCase(n.getNodeName()))
			{
				for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if("skill".equalsIgnoreCase(d.getNodeName()))
					{
						if(it.dispelOnExit == null)
							it.dispelOnExit = new GArray<>();

						int skillId = XmlUtil.getIntAttribute(d, "index", 0);
						if(skillId == 0)
							skillId = XmlUtil.getIntAttribute(d, "skillId", 0);
						else
							skillId = skillId >> 16;

						if(skillId > 0 && !it.dispelOnExit.contains(skillId))
							it.dispelOnExit.add(skillId);
					}
				}
			}
			else if("spawnlist".equalsIgnoreCase(n.getNodeName()))
			{
				for(Node s = n.getFirstChild(); s != null; s = s.getNextSibling())
				{
					if("spawn".equalsIgnoreCase(s.getNodeName()))
					{
						int npcId = 0;
						if(s.getAttributes().getNamedItem("npcId") != null)
							npcId = Integer.parseInt(s.getAttributes().getNamedItem("npcId").getNodeValue());
						List<Integer> randomNpcList = null;
						if(s.getAttributes().getNamedItem("randomNpcId") != null)
						{
							randomNpcList = new FastList<Integer>();
							try
							{
								String[] list = s.getAttributes().getNamedItem("randomNpcId").getNodeValue().split(",");

								for(String val : list)
									randomNpcList.add(Integer.parseInt(val.trim()));
							}
							catch(Exception e)
							{
								randomNpcList = null;
							}
						}

						if(s.getAttributes().getNamedItem("event") != null)
						{
							int delay = s.getAttributes().getNamedItem("delay") != null ? Integer.parseInt(s.getAttributes().getNamedItem("delay").getNodeValue()) : 30;
							it.addSpawn(new InstanceSpawn(s.getAttributes().getNamedItem("event").getNodeValue(), npcId, null, 0, 0, delay, 0, null, 0));
						}
						else
						{
							Location loc = new Location(s.getAttributes().getNamedItem("x") != null ? Integer.parseInt(s.getAttributes().getNamedItem("x").getNodeValue()) : 0, s.getAttributes().getNamedItem("y") != null ? Integer.parseInt(s.getAttributes().getNamedItem("y").getNodeValue()) : 0, s.getAttributes().getNamedItem("z") != null ? Integer.parseInt(s.getAttributes().getNamedItem("z").getNodeValue()) : 0, s.getAttributes().getNamedItem("heading") != null ? Integer.parseInt(s.getAttributes().getNamedItem("heading").getNodeValue()) : Rnd.get(65535));
							int location = s.getAttributes().getNamedItem("location") != null ? Integer.parseInt(s.getAttributes().getNamedItem("location").getNodeValue()) : 0;
							int count = s.getAttributes().getNamedItem("count") != null ? Integer.parseInt(s.getAttributes().getNamedItem("count").getNodeValue()) : 1;
							int respawn = s.getAttributes().getNamedItem("respawn") != null ? Integer.parseInt(s.getAttributes().getNamedItem("respawn").getNodeValue()) : 0;
							int radius = s.getAttributes().getNamedItem("radius") != null ? Integer.parseInt(s.getAttributes().getNamedItem("radius").getNodeValue()) : 0;
							int delay = s.getAttributes().getNamedItem("delay") != null ? Integer.parseInt(s.getAttributes().getNamedItem("delay").getNodeValue()) : 30;
							it.addSpawn(new InstanceSpawn(null, npcId, loc, count, respawn, delay, radius, randomNpcList, location));
						}
					}
					else if("door".equalsIgnoreCase(s.getNodeName()))
						it.addDoor(Integer.parseInt(s.getAttributes().getNamedItem("id").getNodeValue()), s.getAttributes().getNamedItem("open") != null && "true".equalsIgnoreCase(s.getAttributes().getNamedItem("open").getNodeValue()));
				}
			}
			else if("restartPoints".equalsIgnoreCase(n.getNodeName()))
			{
				NamedNodeMap attrs;
				for(Node ed = n.getFirstChild(); ed != null; ed = ed.getNextSibling())
				{
					if("point".equalsIgnoreCase(ed.getNodeName()))
					{
						attrs = ed.getAttributes();
						int x, y, z;
						try
						{
							x = Integer.parseInt(attrs.getNamedItem("x").getNodeValue());
							y = Integer.parseInt(attrs.getNamedItem("y").getNodeValue());
							z = Integer.parseInt(attrs.getNamedItem("z").getNodeValue());
						}
						catch(Exception e)
						{
							continue;
						}

						z = GeoEngine.getHeight(x, y, z, 0) + 5;

						if(it._restartPoints == null)
							it._restartPoints = new GArray<Location>();

						it._restartPoints.add(new Location(x, y, z));
					}
				}
			}
			else if("areaList".equalsIgnoreCase(n.getNodeName()))
			{
				for(Node a = n.getFirstChild(); a != null; a = a.getNextSibling())
				{
					if("area".equals(a.getNodeName()))
					{
						String areaName = a.getAttributes().getNamedItem("name").getNodeValue();
						if(areaName != null)
						{
							if(it._areaList == null)
								it._areaList = new GArray<>();

							it._areaList.add(areaName);
						}
					}
				}

			}
		}

		return it;
	}

	public void setMaxCount(int max)
	{
		_maxCount = max;
	}

	public void setMinParty(int minParty)
	{
		_minParty = minParty;
	}

	public void setMaxParty(int maxParty)
	{
		_maxParty = maxParty;
	}

	public void setMinLevel(int minLevel)
	{
		_minLevel = minLevel;
	}

	public void setMaxLevel(int maxLevel)
	{
		_maxLevel = maxLevel;
	}

	public void setTimeLimit(long timeLimit)
	{
		_timeLimit = timeLimit;
	}

	public void addStartLoc(Location loc)
	{
		if(_startLoc == null)
			_startLoc = new GArray<>(1);

		_startLoc.add(loc);
	}
	
	public void addEndLoc(Location loc)
	{
		if(_endLoc == null)
			_endLoc = new GArray<>(1);

		_endLoc.add(loc);
	}

	public void addSpawn(InstanceSpawn is)
	{
		_spawns.add(is);
	}
	
	public List<InstanceSpawn> getSpawns()
	{
		return _spawns;
	}
	
	public int getMaxCount()
	{
		return _maxCount;
	}

	public int getMinParty()
	{
		return _minParty;
	}

	public int getMaxParty()
	{
		return _maxParty;
	}

	public int getMinLevel()
	{
		return _minLevel;
	}

	public int getMaxLevel()
	{
		return _maxLevel;
	}

	public long getTimeLimit()
	{
		return _timeLimit;
	}

	public long getCoolTime()
	{
		return _coolTime;
	}

	public long getNoUserTimeout()
	{
		return _noUserTimeout;
	}

	public Location getStartLoc()
	{
		if(_startPosType == 0)
			return _startLoc.get(0);

		return _startLoc.get(Rnd.get(_startLoc.size()));
	}

	public Location getEndLoc()
	{
		if(_endLoc == null || _endLoc.size() < 1)
			return null;

		return _endLoc.get(Rnd.get(_endLoc.size()));
	}

	public String getName()
	{
		return _name;
	}

	public int getId()
	{
		return _id;	
	}

	public void registerZone(L2Zone zone)
	{
		_zone = zone;
	}

	public L2Zone getZone()
	{
		return _zone;
	}

	public void addDoor(int doorId, boolean open)
	{
		_doors.put(doorId, open);
	}

	public Map<Integer, Boolean> getDoors()
	{
		return _doors;
	}

	public void setClassName(String name)
	{
		_className = name;
	}

	public String getClassName()
	{
		return _className;
	}

	public int getType()
	{
		return _type;
	}

	public boolean isDispelBuff()
	{
		return _dispel;
	}

	public long getNextTimeUsage(boolean premium)
	{
		if(premium)
			return _premiumReuse.timeNextUsage(System.currentTimeMillis());

		return _reuseReset.timeNextUsage(System.currentTimeMillis());
	}

	public GArray<Location> getRestartPoints()
	{
		return _restartPoints;
	}

	public void addMaker(DefaultMaker maker)
	{
		if(_makers == null)
			_makers = new FastMap<>();

		_makers.put(maker.name, maker);
	}

	public Map<String, DefaultMaker> getMakers()
	{
		return _makers;
	}

	public int getCheckQuest()
	{
		return _checkQuest;
	}

	public GArray<String> getAreaList()
	{
		return _areaList;
	}

	public GArray<Integer> getDispelOnExit()
	{
		return dispelOnExit;
	}
}
