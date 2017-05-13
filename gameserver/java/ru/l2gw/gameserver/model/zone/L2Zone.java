package ru.l2gw.gameserver.model.zone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.listeners.L2ZoneEnterLeaveListener;
import ru.l2gw.extensions.listeners.engine.DefaultListenerEngine;
import ru.l2gw.extensions.listeners.engine.ListenerEngine;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.FestivalManager;
import ru.l2gw.gameserver.model.zone.form.Shape;
import ru.l2gw.gameserver.serverpackets.EventTrigger;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.tables.ReflectionTable;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Location;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author rage
 */
public abstract class L2Zone
{
	protected final GArray<L2Character> _characterList = new GArray<>(32);

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	protected Shape[] _shapes;
	protected Shape[] _exShapes;

	protected int _zoneId;
	private int _minLvl = 0;
	private int _maxLvl = 255;
	protected int _messageNo = 0;
	protected int _enterMessage = 0;
	protected int _exitMessage = 0;
	protected int _mpRegenBonus = 0;
	protected int _hpRegenBonus = 0;
	protected int _moveBonus = 0;
	protected int _damageHp = 0;
	protected int _damageMp = 0;
	protected int _taxById = 0;
	protected int _eventId = 0;
	protected boolean _levelUpRestoreHpMp = true;
	protected double _expLoss = 1;
	protected long _restartTime = 0;
	protected long _initDelay = 0;
	private int[] _class = null;
	private byte _affectClassType = 0;
	private String _zoneName;
	private String _affectRace = null;
	protected String _blockedActions = "";
	protected boolean _active = true;
	protected HashMap<Integer, Boolean> _activeReflections;
	protected ZoneTarget _zoneTarget = ZoneTarget.pc;
	protected GArray<ZoneEffect> _zoneEffects = null;

	protected GArray<Location> _restartPoints = null;
	protected GArray<Location> _pkRestartPoints = null;
	private int _spawnId = 0;
	protected int _entityId = 0;
	protected final GArray<ZoneType> _zoneTypes = new GArray<ZoneType>(1);
	private ListenerEngine<L2Zone> listenerEngine;
	private StatsSet _extAttrs;

	protected static final Log _log = LogFactory.getLog(L2Zone.class.getName());

	public static final String BLOCKED_ACTION_PRIVATE_STORE = "private store";
	public static final String BLOCKED_ACTION_PRIVATE_WORKSHOP = "private workshop";
	public static final String BLOCKED_MINI_MAP = "mini map";
	public static final String BLOCKED_SKILL_RESURRECT = "resurrect";
	public static final String BLOCKED_ITEM_DROP = "drop item";

	/**
	 * Zone system
	 */
	public static enum EventType
	{
		ONENTER, ONEXIT, SCHEDULE
	}

	public static enum ZoneTarget
	{
		pc,
		only_pc,
		npc
	}

	public static enum ZoneType
	{
		altered,
		battle,
		danger,
		dummy,
		instance,
		fishing,
		headquarters,
		offshore,
		olympiad_stadia,
		landing,
		no_spawn,
		no_escape,
		no_restart,
		no_landing,
		no_summon,
		peace,
		siege,
		siege_residence,
		residence,
		ssq,
		town,
		water,
		no_radar,
		trap_area,
		jail
	}

	public void register()
	{
		if(_zoneTypes.contains(ZoneType.ssq) && _entityId > 0)
			FestivalManager.getInstance().addZone(this);
		else if(_zoneTypes.contains(ZoneType.instance) && _entityId != 0)
			InstanceManager.getInstance().addZone(this);
	}

	public static L2Zone parseZone(Node zn)
	{
		String classType = "Default";
		String zoneName = "";
		int id = 0;
		L2Zone zone;
		Class<?> clazz;
		Constructor<?> constructor;

		try
		{
			id = Integer.parseInt(zn.getAttributes().getNamedItem("id").getNodeValue());
			Node tn = zn.getAttributes().getNamedItem("classType");
			Node nn = zn.getAttributes().getNamedItem("name");
			if(tn != null)
				classType = tn.getNodeValue();

			zoneName = (nn != null) ? nn.getNodeValue() : "Zone " + Integer.toString(id);

			clazz = Class.forName("ru.l2gw.gameserver.model.zone.L2" + classType + "Zone");
			constructor = clazz.getConstructor();
			zone = (L2Zone) constructor.newInstance();
		}
		catch(Exception e)
		{
			_log.warn("Cannot create a L2" + classType + "Zone for id " + id);
			return null;
		}

		zone._zoneId = id;
		zone._zoneName = zoneName;

		GArray<Shape> shapes = new GArray<Shape>(1);
		GArray<Shape> exShapes = new GArray<Shape>(1);
		NamedNodeMap attrs;
		for(Node n = zn.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if("attr".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				String name = attrs.getNamedItem("name").getNodeValue();
				String val = attrs.getNamedItem("val").getNodeValue();
				zone.setAttribute(name, val);
			}
			else if("shape".equalsIgnoreCase(n.getNodeName()))
			{
				Shape sh = Shape.parseShape(n, id);
				if(sh != null)
				{
					if(sh.isExclude())
						exShapes.add(sh);
					else
						shapes.add(sh);
				}
				else
					return null;
			}
			else if("effects".equalsIgnoreCase(n.getNodeName()))
			{
				try
				{
					zone.parseEffects(n);
				}
				catch(Exception e)
				{
					_log.warn("Cannot parse spawnlist for zone " + zone.getZoneName() + "[" + zone.getZoneId() + "]");
					return null;
				}
			}
			else if("restartPoints".equalsIgnoreCase(n.getNodeName()))
			{
				try
				{
					zone.parseRestartPoints(n, false);
				}
				catch(Exception e)
				{
					_log.warn("Cannot parse spawnlist for zone " + zone.getZoneName() + "[" + zone.getZoneId() + "]");
					return null;
				}
			}
			else if("restartChaotic".equalsIgnoreCase(n.getNodeName()))
			{
				try
				{
					zone.parseRestartPoints(n, true);
				}
				catch(Exception e)
				{
					_log.warn("Cannot parse spawnlist for zone " + zone.getZoneName() + "[" + zone.getZoneId() + "]");
					return null;
				}
			}
			else if("listener".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				String name = attrs.getNamedItem("name").getNodeValue();
				Constructor<?> ListenerConstructor = null;
				try
				{
					ListenerConstructor = Class.forName("ru.l2gw.extensions.listeners.L2Zone." + name).getConstructors()[0];
				}
				catch(final ClassNotFoundException e)
				{
					e.printStackTrace();
					_log.warn("cannot found listenerClass " + name + " for: " + zone.getZoneId() + " skipped");
				}
				if(ListenerConstructor != null)
				{
					try
					{
						zone.getListenerEngine().addMethodInvokedListener((L2ZoneEnterLeaveListener) ListenerConstructor.newInstance());
					}
					catch(final Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}

		zone._shapes = shapes.toArray(new Shape[shapes.size()]);
		if(exShapes.size() > 0)
			zone._exShapes = exShapes.toArray(new Shape[exShapes.size()]);
		shapes.clear();
		exShapes.clear();

		if(zone._zoneTypes.size() < 1)
			zone._zoneTypes.add(ZoneType.dummy);

		//zone.register();

		return zone;
	}

	private void parseRestartPoints(Node n, boolean pk) throws Exception
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
					_log.warn(this + ": Cannot parse spawn point!");
					e.printStackTrace();
					continue;
				}

				z = GeoEngine.getHeight(x, y, z, 0) + 5;

				if(pk)
					addPkRestartPoint(new Location(x, y, z));
				else
					addRestartPoint(new Location(x, y, z));
			}
		}
		if(Config.ZONE_DEBUG && (_pkRestartPoints != null || _restartPoints != null))
		{
			if(pk)
				_log.warn(this + ": chaotic restart points loaded: " + _pkRestartPoints.size());
			else
				_log.warn(this + ": restart points loaded: " + _restartPoints.size());
		}
	}

	private void parseEffects(Node n) throws Exception
	{
		NamedNodeMap attrs;
		for(Node ed = n.getFirstChild(); ed != null; ed = ed.getNextSibling())
		{
			if("effect".equalsIgnoreCase(ed.getNodeName()))
			{
				attrs = ed.getAttributes();
				String action = attrs.getNamedItem("action").getNodeValue();

				ZoneEffect ze;
				if(action.equalsIgnoreCase("add"))
					ze = new ZoneEffect(0);
				else if(action.equalsIgnoreCase("remove"))
					ze = new ZoneEffect(1);
				else if(action.equalsIgnoreCase("teleport"))
				{
					ze = new ZoneEffect(2);
					ze.setTeleportPoint(Location.parseLoc(attrs.getNamedItem("teleport_points").getNodeValue()));
				}
				else
				{
					_log.warn(this + ": can't parse zone effect. Unknown action type: " + action);
					continue;
				}

				String event = attrs.getNamedItem("event").getNodeValue();
				if(event.equalsIgnoreCase("onexit"))
					ze.setEvent(EventType.ONEXIT);
				else if(event.equalsIgnoreCase("schedule"))
				{
					ze.setEvent(EventType.SCHEDULE);
					String rate = attrs.getNamedItem("unit_tick").getNodeValue();
					try
					{
						ze.setRate(Integer.parseInt(rate));
					}
					catch(Exception e)
					{
					}
				}
				else
					ze.setEvent(EventType.ONENTER);

				try
				{
					if(attrs.getNamedItem("probe") != null)
						ze.setProbe(Integer.parseInt(attrs.getNamedItem("probe").getNodeValue()));
					if(attrs.getNamedItem("dayTime") != null)
						ze.setDayTime(attrs.getNamedItem("dayTime").getNodeValue());
				}
				catch(Exception e)
				{
				}

				for(Node sd = ed.getFirstChild(); sd != null; sd = sd.getNextSibling())
				{
					if("skill".equalsIgnoreCase(sd.getNodeName()))
					{
						attrs = sd.getAttributes();
						String skillId = attrs.getNamedItem("id").getNodeValue();
						String level = attrs.getNamedItem("level").getNodeValue();

						try
						{
							int sid = Integer.parseInt(skillId);
							int lvl = Integer.parseInt(level);
							ze.addSkill(sid, lvl);
						}
						catch(Exception e)
						{
						}
					}
				}
				addEffect(ze);
			}
		}
		if(Config.ZONE_DEBUG)
			_log.warn(this + ": effects loaded: " + getZoneEffects().size());
	}


	/**
	 * Setup new parameters for this zone
	 *
	 * @param name
	 * @param value
	 */
	public void setAttribute(String name, String value)
	{

		if(name.equalsIgnoreCase("type"))
			_zoneTypes.add(ZoneType.valueOf(value));
			// Minimum leve
		else if(name.equals("affectedLvlMin"))
			_minLvl = Integer.parseInt(value);
			// Maximum level
		else if(name.equals("affectedLvlMax"))
			_maxLvl = Integer.parseInt(value);
			// Affected Races
		else if(name.equals("affectedRace"))
			_affectRace = value;
			// Affected classes
		else if(name.equals("affectedClassId"))
		{
			if(_class == null)
			{
				_class = new int[1];
				_class[0] = Integer.parseInt(value);
			}
			else
			{
				int[] temp = new int[_class.length + 1];

				int i = 0;
				for(; i < _class.length; i++)
					temp[i] = _class[i];

				temp[i] = Integer.parseInt(value);

				_class = temp;
			}
		}
		// Affected class type
		else if(name.equals("affectedClassType"))
			_affectClassType = (byte) (value.equals("Fighter") ? 1 : 2);
		else if(name.equalsIgnoreCase("blockedActions"))
			_blockedActions = value;
		else if(name.equals("taxById"))
			_taxById = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("active"))
			_active = !value.equalsIgnoreCase("false");
		else if(name.equalsIgnoreCase("entityId"))
			_entityId = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("messageNo"))
			_messageNo = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("enterMessageNo"))
			_enterMessage = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("exitMessageNo"))
			_exitMessage = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("mpRegenBonus"))
			_mpRegenBonus = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("hpRegenBonus"))
			_hpRegenBonus = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("damageHp"))
			_damageHp = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("damageMp"))
			_damageMp = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("moveBonus"))
			_moveBonus = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("expLoss"))
			_expLoss = Double.parseDouble(value);
		else if(name.equalsIgnoreCase("event_id"))
			_eventId = Integer.parseInt(value);
		else if(name.equalsIgnoreCase("levelUpRestoreHPMP"))
			_levelUpRestoreHpMp = value.equalsIgnoreCase("true");
		else if(name.equalsIgnoreCase("initialDelay"))
			_initDelay = Integer.parseInt(value) * 1000;
		else if(name.equalsIgnoreCase("target"))
			_zoneTarget = ZoneTarget.valueOf(value);
		else if(name.equalsIgnoreCase("restartTime"))
			_restartTime = Integer.parseInt(value);
		else
		{
			if(_extAttrs == null)
				_extAttrs = new StatsSet();

			_extAttrs.set(name, value);
		}
	}

	/**
	 * Checks if the given character is affected by this zone
	 *
	 * @param character
	 * @return boolean
	 */
	protected boolean isAffected(L2Character character)
	{
		if(!checkTarget(character))
			return false;
		// Check lvl
		if(character.getLevel() < _minLvl || character.getLevel() > _maxLvl)
			return false;

		if(character instanceof L2Player)
		{
			L2Player player = (L2Player) character;
			// Check class type
			if(_affectClassType != 0)
				return (player.isMageClass() && _affectClassType == 1) || (!player.isMageClass() && _affectClassType == 2);

			// Check race
			if(_affectRace != null)
				return _affectRace.equalsIgnoreCase("all") || _affectRace.contains(player.getRace().toString());

			// Check class
			if(_class != null)
			{
				for(int classId : _class)
					if(player.getClassId().ordinal() == classId)
						return true;

				return false;
			}
		}
		return true;
	}

	/**
	 * Проверяет подходит ли обьект для вызвавшего действия
	 *
	 * @param object обьект
	 * @return подошел ли
	 */
	private boolean checkTarget(L2Character object)
	{
		switch(_zoneTarget)
		{
			case pc:
				return object.isPlayer() || object.isSummon() || object.isPet();
			case only_pc:
				return object.isPlayer();
			case npc:
				return object.isNpc();
		}
		return false;
	}

	public Shape[] getShapes()
	{
		return _shapes;
	}

	public Shape[] getExShapes()
	{
		return _exShapes;
	}

	/**
	 * Checks if the given coordinates are within the zone
	 *
	 * @param x
	 * @param y
	 */
	public boolean isInsideZone(int x, int y)
	{
		boolean inside = false;
		for(Shape sh : _shapes)
		{
			if(sh.contains(x, y))
			{
				inside = true;
				break;
			}
		}

		if(inside && _exShapes != null)
		{
			for(Shape sh : _exShapes)
			{
				if(sh.contains(x, y))
				{
					inside = false;
					break;
				}
			}
		}
		return inside;
	}

	/**
	 * Checks if the given coordinates are within the zone
	 *
	 * @param x
	 * @param y
	 * @param z
	 */
	public boolean isInsideZone(int x, int y, int z)
	{
		boolean inside = false;
		for(Shape sh : _shapes)
		{
			if(sh.contains(x, y, z))
			{
				inside = true;
				break;
			}
		}

		if(inside && _exShapes != null)
		{
			for(Shape sh : _exShapes)
			{
				if(sh.contains(x, y, z))
				{
					inside = false;
					break;
				}
			}
		}
		return inside;
	}

	/**
	 * Checks if the given obejct is inside the zone.
	 *
	 * @param cha
	 */
	public boolean isInsideZone(L2Character cha)
	{
		return isInsideZone(cha.getX(), cha.getY(), cha.getZ());
	}

	public double getDistanceToZone(int x, int y)
	{
		double dist = Double.MAX_VALUE;
		for(Shape sh : _shapes)
		{
			dist = Math.min(dist, sh.getDistanceToZone(x, y));
		}
		return dist;
	}

	public double getDistanceToZone(L2Object object)
	{
		return getDistanceToZone(object.getX(), object.getY());
	}

	public boolean intersectsRectangle(int ax, int bx, int ay, int by)
	{
		for(Shape sh : _shapes)
		{
			if(sh.intersectsRectangle(ax, bx, ay, by))
				return true;
		}
		return false;
	}

	public int getMaxZ(L2Object obj)
	{
		return getMaxZ(obj.getX(), obj.getY(), obj.getZ());
	}

	public int getMinZ(L2Object obj)
	{
		return getMinZ(obj.getX(), obj.getY(), obj.getZ());
	}

	public int getMaxZ(int x, int y, int z)
	{
		for(Shape sh : _shapes)
		{
			if(sh.contains(x, y))
				return sh.getMaxZ();
		}
		return z;
	}

	public int getMinZ(int x, int y, int z)
	{
		for(Shape sh : _shapes)
		{
			if(sh.contains(x, y))
				return sh.getMinZ();
		}
		return z;
	}

	public int getMinZ()
	{
		int z = Integer.MAX_VALUE;

		if(_shapes != null)
		{
			for(Shape sh : _shapes)
			{
				if(sh.checkZ() && z > sh.getMinZ())
					z = sh.getMinZ();
			}
		}

		if(_exShapes != null)
		{
			for(Shape sh : _exShapes)
			{
				if(sh.checkZ() && z > sh.getMinZ())
					z = sh.getMinZ();
			}
		}
		return z;
	}

	public int getMaxZ()
	{
		int z = Integer.MIN_VALUE;

		if(_shapes != null)
		{
			for(Shape sh : _shapes)
			{
				if(sh.checkZ() && z < sh.getMaxZ())
					z = sh.getMaxZ();
			}
		}

		if(_exShapes != null)
		{
			for(Shape sh : _exShapes)
			{
				if(sh.checkZ() && z < sh.getMaxZ())
					z = sh.getMaxZ();
			}
		}
		return z;
	}

	public int getMinX()
	{
		int x = Integer.MAX_VALUE;

		if(_shapes != null)
			for(Shape sh : _shapes)
				if(x > sh.getXMin())
					x = sh.getXMin();

		return x;
	}

	public int getMaxX()
	{
		int x = Integer.MIN_VALUE;

		if(_shapes != null)
			for(Shape sh : _shapes)
				if(x < sh.getXMax())
					x = sh.getXMax();

		return x;
	}

	public int getMinY()
	{
		int y = Integer.MAX_VALUE;

		if(_shapes != null)
			for(Shape sh : _shapes)
				if(y > sh.getYMin())
					y = sh.getYMin();

		return y;
	}

	public int getMaxY()
	{
		int y = Integer.MIN_VALUE;

		if(_shapes != null)
			for(Shape sh : _shapes)
				if(y < sh.getYMax())
					y = sh.getYMax();

		return y;
	}

	public boolean checkZ()
	{
		if(_shapes != null)
			for(Shape sh : _shapes)
			{
				if(!sh.checkZ())
					return false;
			}

		if(_exShapes != null)
			for(Shape sh : _exShapes)
			{
				if(!sh.checkZ())
					return false;
			}

		return true;
	}


	public void doEnter(L2Character character)
	{
		boolean added = false;

		writeLock.lock();
		try
		{
			if(!_characterList.contains(character))
				added = _characterList.add(character);
		}
		finally
		{
			writeLock.unlock();
		}

		if(added)
			onEnter(character);
	}

	public void doExit(L2Character character)
	{
		boolean removed = false;
		writeLock.lock();
		try
		{
			removed = _characterList.remove(character);
		}
		finally
		{
			writeLock.unlock();
		}

		if(removed)
			onExit(character);
	}

	/**
	 * Will scan the zones char list for the character
	 *
	 * @param character
	 * @return boolean
	 */
	public boolean isCharacterInZone(L2Character character)
	{
		readLock.lock();
		try
		{
			return _characterList.contains(character);
		}
		finally
		{
			readLock.unlock();
		}
	}

	public abstract void onEnter(L2Character character);

	public abstract void onExit(L2Character character);

	public String getZoneName()
	{
		if(_zoneName == null || _zoneName.length() < 1)
			return "not defined";
		return _zoneName;
	}

	public void addEffect(ZoneEffect ze)
	{
		if(_zoneEffects == null)
			_zoneEffects = new GArray<ZoneEffect>(1);
		_zoneEffects.add(ze);
	}

	public GArray<ZoneEffect> getZoneEffects()
	{
		return _zoneEffects;
	}

	public void telePlayers()
	{
		for(L2Player player : getPlayers())
			player.teleToClosestTown();
	}

	public void telePlayers(Location loc)
	{
		for(L2Player player : getPlayers())
			player.teleToLocation(loc);
	}

	public GArray<L2Character> getCharacters()
	{
		GArray<L2Character> result;
		readLock.lock();
		try
		{
			result = new GArray<>(_characterList.size());
			result.addAll(_characterList);
		}
		finally
		{
			readLock.unlock();
		}
		return result;
	}

	public GArray<L2Player> getPlayers()
	{
		GArray<L2Player> players;
		readLock.lock();
		try
		{
			players = new GArray<>(_characterList.size());
			for(L2Character cha : _characterList)
				if(cha instanceof L2Player)
					players.add((L2Player) cha);
		}
		finally
		{
			readLock.unlock();
		}

		return players;
	}

	public int getZoneId()
	{
		return _zoneId;
	}

	public void addRestartPoint(Location loc)
	{
		if(_restartPoints == null)
			_restartPoints = new GArray<Location>(1);

		if(loc != null)
			_restartPoints.add(loc);
	}

	public void addPkRestartPoint(Location loc)
	{
		if(_pkRestartPoints == null)
			_pkRestartPoints = new GArray<Location>(1);

		if(loc != null)
			_pkRestartPoints.add(loc);
	}

	public final Location getSpawn()
	{
		return getSpawn(null);
	}

	public final Location getSpawn(L2Character cha)
	{
		if(Config.ZONE_DEBUG)
			_log.info("Zone: getSapwn");

		if(cha instanceof L2Player)
		{
			L2Player player = (L2Player) cha;
			Location loc = player.getStablePoint();
			if(loc != null)
			{
				player.setStablePoint(null);
				return loc;
			}
		}

		if(cha != null && cha.getKarma() > 1)
			return getPKSpawn();

		if(_restartPoints != null && _restartPoints.size() > 0)
		{
			if(_spawnId >= _restartPoints.size()) _spawnId = 0;
			if(Config.ZONE_DEBUG)
				_log.info("Zone: spawnId " + _spawnId);
			Location loc = _restartPoints.get(_spawnId);
			if(Config.ZONE_DEBUG)
				_log.info("Zone: loc " + loc.getX() + " " + loc.getY() + " " + loc.getZ());
			_spawnId++;
			return loc;
		}
		_log.warn(this + " no restart point defined, teleport to floran!");
		return new Location(17817, 170079, -3530);
	}

	public final Location getPKSpawn()
	{
		if(Config.ZONE_DEBUG)
			_log.info("Zone: getSapwn");
		if(_pkRestartPoints != null && _pkRestartPoints.size() > 0)
			return _pkRestartPoints.get(Rnd.get(_pkRestartPoints.size()));
		else
			return getSpawn(null);
	}

	public GArray<Location> getRestartPoints()
	{
		return _restartPoints;
	}

	public boolean isActive()
	{
		return _active;
	}

	public boolean isActive(int refId)
	{
		readLock.lock();
		try
		{
			return _active || _activeReflections != null && _activeReflections.containsKey(refId) && _activeReflections.get(refId);
		}
		finally
		{
			readLock.unlock();
		}
	}

	public void setActive(boolean active)
	{
		writeLock.lock();
		try
		{
			if(_active != active)
			{
				if(Config.ZONE_DEBUG)
					_log.info(this + " change active from " + _active + " to " + active);
				if(active)
				{
					_active = active;
					for(L2Character cha : _characterList)
						onEnter(cha);
				}
				else
				{
					for(L2Character cha : _characterList)
					{
						if(_eventId > 0 && cha.isPlayer())
							cha.sendPacket(new EventTrigger(_eventId, false));
						onExit(cha);
					}
					_active = active;
				}
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public void setActive(boolean active, int refId)
	{
		writeLock.lock();
		try
		{
			if(_activeReflections == null)
				_activeReflections = new HashMap<>();

			if((_activeReflections.get(refId) == null && active) || (_activeReflections.get(refId) != null && _activeReflections.get(refId) != active))
			{
				if(Config.ZONE_DEBUG)
					_log.info(this + " change active from " + (_activeReflections.get(refId) != null && _activeReflections.get(refId)) + " to " + active + " refId=" + refId + ";");
				if(active)
				{
					if(!_activeReflections.containsKey(refId))
						ReflectionTable.getInstance().getById(refId).addZone(this);

					_activeReflections.put(refId, active);

					for(L2Character cha : _characterList)
						if(cha.getReflection() == refId)
							onEnter(cha);
				}
				else
				{
					for(L2Character cha : _characterList)
						if(cha.getReflection() == refId)
						{
							if(_eventId > 0 && cha.isPlayer())
								cha.sendPacket(new EventTrigger(_eventId, false));
							onExit(cha);
						}

					_activeReflections.put(refId, active);
				}
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public void onReflectionCollapse(int refId)
	{
		setActive(false, refId);
		writeLock.lock();
		try
		{
			_activeReflections.remove(refId);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public GArray<ZoneType> getTypes()
	{
		return _zoneTypes;
	}

	public boolean isActionBlocked(String action)
	{
		return _blockedActions != null && _blockedActions.contains(action);
	}

	public String getBlockedActions()
	{
		return _blockedActions;
	}

	public int getEntityId()
	{
		return _entityId;
	}

	public int getTaxById()
	{
		return _taxById;
	}

	public ListenerEngine<L2Zone> getListenerEngine()
	{
		if(listenerEngine == null)
			listenerEngine = new DefaultListenerEngine<L2Zone>(this);
		return listenerEngine;
	}

	protected class ZoneEffect
	{
		private int _action; // 0 - add, 1 - remove, 2 - teleport
		private Map<Integer, Integer> _skills;
		private int _rate;
		private int _probe = 0;
		private int _dayTime = 0;
		private Location _teleportPoint;

		private EventType _event = EventType.ONENTER;

		public ZoneEffect(int act)
		{
			_action = act;
		}

		public int getAction()
		{
			return _action;
		}

		public void addSkill(int skillId, int level)
		{
			if(_skills == null)
				_skills = new HashMap<>();
			_skills.put(skillId, level);
		}

		public Set<Integer> getSkillIds()
		{
			return _skills.keySet();
		}

		public int getSkillLevel(int skillId)
		{
			if(_skills != null) return _skills.get(skillId);
			return 0;
		}

		public void setEvent(EventType event)
		{
			_event = event;
		}

		public EventType getEvent()
		{
			return _event;
		}

		public void setRate(int rate)
		{
			_rate = rate * 666;
		}

		public long getRate()
		{
			return _rate;
		}

		public int getProbe()
		{
			return _probe;
		}

		public void setProbe(int probe)
		{
			_probe = probe;
		}

		public int getDayTime()
		{
			return _dayTime;
		}

		public void setDayTime(String dayTime)
		{
			if(dayTime.equalsIgnoreCase("night"))
				_dayTime = 1;
			else if(dayTime.equalsIgnoreCase("day"))
				_dayTime = 2;
			else
				_dayTime = 0;
		}

		public void setTeleportPoint(Location loc)
		{
			_teleportPoint = loc;
		}

		public Location getTeleportPoint()
		{
			return _teleportPoint;
		}
	}

	public long getRestartTime()
	{
		return _restartTime;
	}

	public double getExpLoss()
	{
		return _expLoss;
	}

	public boolean isLevelUpRestoreHpMp()
	{
		return _levelUpRestoreHpMp;
	}

	public StatsSet getExtAttributes()
	{
		return _extAttrs;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getZoneId() + "] " + getZoneName();
	}

	public void broadcastPacket(L2GameServerPacket gsp)
	{
		for(L2Character cha : getPlayers())
			cha.sendPacket(gsp);
	}
}
