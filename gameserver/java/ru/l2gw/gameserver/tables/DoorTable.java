package ru.l2gw.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.listeners.DayNightChangeListener;
import ru.l2gw.extensions.listeners.DoorOpenCloseListener;
import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.model.L2DoorStatus;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.templates.L2CharTemplate;
import ru.l2gw.gameserver.templates.StatsSet;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class DoorTable
{
	private static final Log _log = LogFactory.getLog(DoorTable.class.getName());

	private TIntObjectHashMap<L2DoorInstance> _staticItems;

	static ScheduledFuture<AutoOpenCloseDoors> _autoOpenTask;

	static TIntObjectHashMap<L2DoorStatus> _doorstatus;

	private static DoorTable _instance;

	private static DayNightListener _dayNightListiner;

	public static DoorTable getInstance()
	{
		if(_instance == null)
			new DoorTable();
		return _instance;
	}

	public DoorTable()
	{
		_instance = this;
		_staticItems = new TIntObjectHashMap<>();
		restoreDoors();
		_doorstatus = new TIntObjectHashMap<>();
		restoreOpenCloseDoors();
		//запускаем таск через 10 секунд после загрузки дверей.
		ThreadPoolManager.getInstance().scheduleGeneral(new AutoOpenCloseDoors(), 10);
		_dayNightListiner = new DayNightListener();
		GameTimeController.getInstance().getListenerEngine().addPropertyChangeListener(_dayNightListiner);
	}

	public void respawn()
	{
		_staticItems = null;
		_instance = null;
		_instance = new DoorTable();
	}

	private void restoreOpenCloseDoors()
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			File file = new File("./data/doors_group.xml");
			if(!file.exists())
			{
				if(Config.DEBUG)
					System.out.println("doors_group.xml: NO FILE");
				return;
			}

			Document doc = factory.newDocumentBuilder().parse(file);
			int counterDS = 0;
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if("list".equalsIgnoreCase(n.getNodeName()))
				{
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if("group".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							String onstart = (String.valueOf(attrs.getNamedItem("onstart").getNodeValue()));
							String openEvent = attrs.getNamedItem("openEvent") != null ? attrs.getNamedItem("openEvent").getNodeValue() : null;
							String closeEvent = attrs.getNamedItem("closeEvent") != null ? attrs.getNamedItem("closeEvent").getNodeValue() : null;

							int opendelay = 0;
							int closedelay = 0;
							if(attrs.getNamedItem("opendelay") != null)
								opendelay = Integer.parseInt(attrs.getNamedItem("opendelay").getNodeValue());
							if(attrs.getNamedItem("closedelay") != null)
								closedelay = Integer.parseInt(attrs.getNamedItem("closedelay").getNodeValue());
							ArrayList<Integer> doors = new ArrayList<Integer>();
							for(Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if("door".equalsIgnoreCase(cd.getNodeName()))
									doors.add(Integer.parseInt(cd.getAttributes().getNamedItem("id").getNodeValue()));
							}
							L2DoorStatus ds = new L2DoorStatus(counterDS);
							ds.addDoorStatus(id, doors, opendelay, closedelay, onstart, openEvent, closeEvent);
							_doorstatus.put(counterDS, ds);
							//Устанавливаем начальные параметры дверей при старте сервера
							for(Integer doorId : doors)
							{
								if(getDoor(doorId) != null)
								{
									if(ds.isOpen())
										getDoor(doorId).openMe();
									else
										getDoor(doorId).closeMe();
								}
								else
									_log.warn("DoorTable: warning autoOpen can't find door: " + doorId);
							}
							counterDS++;
						}
					}
				}
			}
			if(Config.DEBUG)
				System.out.println("DoorStatus: OK");
		}
		catch(Exception e)
		{
			_log.warn("DoorStatus: Error parsing doors_group.xml file. " + e);
		}
	}

	private void restoreDoors()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM doors");
			rs = statement.executeQuery();
			fillDoors(rs);
		}
		catch(Exception E)
		{
			_log.warn("Cannot load doors");
			E.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	private void fillDoors(ResultSet doorData) throws Exception
	{
		StatsSet baseDat = new StatsSet();
		baseDat.set("level", 0);
		baseDat.set("jClass", "door");
		baseDat.set("baseSTR", 0);
		baseDat.set("baseCON", 0);
		baseDat.set("baseDEX", 0);
		baseDat.set("baseINT", 0);
		baseDat.set("baseWIT", 0);
		baseDat.set("baseMEN", 0);
		baseDat.set("baseShldDef", 0);
		baseDat.set("baseShldRate", 0);
		baseDat.set("baseAccCombat", 38);
		baseDat.set("baseEvasRate", 38);
		baseDat.set("baseCritRate", 38);
		// Добавить в датапак collision_radius.
		// Только не забывать, что в алгоритмах атаки есть минимальный радиус (обычно 40).
		baseDat.set("collision_radius", 5);
		baseDat.set("collision_height", 0);
		baseDat.set("sex", "male");
		baseDat.set("type", "");
		baseDat.set("baseAtkRange", 0);
		baseDat.set("baseMpMax", 0);
		baseDat.set("baseCpMax", 0);
		baseDat.set("revardExp", 0);
		baseDat.set("revardSp", 0);
		baseDat.set("basePAtk", 0);
		baseDat.set("baseMAtk", 0);
		baseDat.set("basePAtkSpd", 0);
		baseDat.set("aggroRange", 0);
		baseDat.set("baseMAtkSpd", 0);
		baseDat.set("rhand", 0);
		baseDat.set("lhand", 0);
		baseDat.set("armor", 0);
		baseDat.set("baseWalkSpd", 0);
		baseDat.set("baseRunSpd", 0);
		baseDat.set("baseHpReg", 3.e-3f);
		baseDat.set("baseCpReg", 0);
		baseDat.set("baseMpReg", 3.e-3f);

		StatsSet npcDat;
		while(doorData.next())
		{
			npcDat = baseDat.clone();
			int id = doorData.getInt("id");
			int zmin = doorData.getInt("minz");
			int zmax = doorData.getInt("maxz");
			int posx = doorData.getInt("posx");
			int posy = doorData.getInt("posy");
			String doorname = doorData.getString("name");

			npcDat.set("npcId", id);
			npcDat.set("name", doorname);
			npcDat.set("baseHpMax", doorData.getInt("hp"));
			npcDat.set("basePDef", doorData.getInt("pdef"));
			npcDat.set("baseMDef", doorData.getInt("mdef"));

			L2CharTemplate template = new L2CharTemplate(npcDat);

			L2DoorInstance door = new L2DoorInstance(IdFactory.getInstance().getNextId(), template, id, doorname, doorData.getBoolean("unlockable"), doorData.getBoolean("destroyable"), doorData.getInt("grade"), doorData.getBoolean("showHp"));
			_staticItems.put(id, door);
			door._geoPos.add(doorData.getInt("ax"), doorData.getInt("ay"), zmin, zmax);
			door._geoPos.add(doorData.getInt("bx"), doorData.getInt("by"), zmin, zmax);
			door._geoPos.add(doorData.getInt("cx"), doorData.getInt("cy"), zmin, zmax);
			door._geoPos.add(doorData.getInt("dx"), doorData.getInt("dy"), zmin, zmax);
			door.getTemplate().collisionHeight = zmax - zmin & 0xfff0;
			door.getTemplate().collisionRadius = Math.min(posx - door._geoPos.getXmin(), posy - door._geoPos.getYmin());
			door.setCloseTime(doorData.getInt("close_time"));

			if(door._geoPos.getXmin() == door._geoPos.getXmax() || door._geoPos.getYmin() == door._geoPos.getYmax())
				_log.warn("door " + id + " has zero size");
			else if(zmax - zmin < 64)
				_log.warn("door " + id + " has too low height");

			door.setXYZInvisible(posx, posy, (zmax + zmin) / 2);
			door.setCurrentHpMp(door.getMaxHp(), door.getMaxMp());
			door.setOpen(false);

			door.spawnMe(door.getLoc());
		}
		_log.info("DoorTable: Loaded " + _staticItems.size() + " doors.");
	}

	public boolean isInitialized()
	{
		return _initialized;
	}

	private boolean _initialized = true;

	public L2DoorInstance getDoor(Integer id)
	{
		return _staticItems.get(id);
	}

	public L2DoorInstance getDoorByName(String name)
	{
		for(L2DoorInstance door : _staticItems.valueCollection())
			if(door.getDoorName().equals(name))
				return door;

		return null;
	}

	public void putDoor(Integer id, L2DoorInstance door)
	{
		_staticItems.put(id, door);
	}

	public void removeDoor(Integer id)
	{
		_staticItems.remove(id);
	}

	public L2DoorInstance[] getDoors()
	{
		return _staticItems.valueCollection().toArray(new L2DoorInstance[_staticItems.size()]);
	}

	private long CalcNextTime(long time, long newtime)
	{
		if(newtime == 0)
			return time;
		if(time == 0)
			return newtime;
		if(time < newtime)
			return time;
		else
			return newtime;
	}

	private class AutoOpenCloseDoors implements Runnable
	{
		public void run()
		{
			runAutoOpenCloseDoors();
		}
	}

	private void runAutoOpenCloseDoors()
	{
		if(_doorstatus.size() == 0)
			return;
		long time = System.currentTimeMillis();
		long nextstart = 0;
		for(int a = 0; a < _doorstatus.size(); a++)
		{
			L2DoorStatus ds = _doorstatus.get(a);
			if(time >= ds.getNextTimer() && ds.getNextTimer() != 0) //Пришло время открытия/закрытия дверей
			{
				ArrayList<Integer> doors = ds.getDoors();
				boolean isOpen = ds.isOpen();
				for(Integer doorId : doors)
				{
					if(getDoor(doorId) != null)
					{
						if(isOpen)
							getDoor(doorId).closeMe();
						else
							getDoor(doorId).openMe();
					}
					else
						_log.warn("DoorTable: warning autoOpen can't find door: " + doorId);
				}
				ds.changeStatus();
			}
			nextstart = CalcNextTime(nextstart, ds.getNextTimer());
		}
		long diff = nextstart - time;
		_autoOpenTask = ThreadPoolManager.getInstance().scheduleGeneral(new AutoOpenCloseDoors(), diff);
	}

	public static void stopAutoOpenTask()
	{
		if(_autoOpenTask != null)
			_autoOpenTask.cancel(true);
	}

	public void notifyEvent(String event)
	{
		for(L2DoorStatus ds : _doorstatus.valueCollection())
			ds.notifyEvent(event);
	}

	public void doorOpenClose(String doorName, int open)
	{
		for(L2DoorInstance door : _staticItems.valueCollection())
			if(door.getDoorName().equalsIgnoreCase(doorName))
			{
				if(open == 0)
				{
					door.openMe();
					door.onOpen();
				}
				else
					door.closeMe();
			}
	}

	public void doorOpenClose(String doorName, int open, int refId)
	{
		Instance inst = InstanceManager.getInstance().getInstanceByReflection(refId);
		if(inst == null)
			return;

		inst.openCloseDoor(doorName, open);
	}

	public void addOpenCloseListener(String doorName, DoorOpenCloseListener listener)
	{
		for(L2DoorInstance door : _staticItems.valueCollection())
			if(door.getDoorName().equalsIgnoreCase(doorName))
			{
				door.getListenerEngine().addMethodInvokedListener(listener);
				return;
			}
	}

	private class DayNightListener extends DayNightChangeListener
	{
		@Override
		public void switchToNight()
		{
			notifyEvent("night");
		}

		@Override
		public void switchToDay()
		{
			notifyEvent("day");
		}
	}
}