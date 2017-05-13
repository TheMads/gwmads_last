package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.database.mysql;
import ru.l2gw.extensions.scripts.Script;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.tables.SpawnTable;
import ru.l2gw.gameserver.tables.TerritoryTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Location;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This class manages the spawn and respawn of a group of L2NpcInstance that are in the same are and have the same type.
 *
 * <B><U> Concept</U> :</B><BR><BR>
 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
 * The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR><BR>
 */
@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class L2Spawn
{
	private static Log _log = LogFactory.getLog(L2Spawn.class.getName());

	/** Минимальное время респа */
	private static final int MIN_RESPAWN_DELAY = 1;

	/** The link on the L2NpcTemplate object containing generic and static properties of this spawn (ex : RewardExp, RewardSP, AggroRange...) */
	private L2NpcTemplate _template;

	/** The Identifier of this spawn in the spawn table */
	private int _id;

	// private String _location = DEFAULT_LOCATION;

	/** The identifier of the location area where L2NpcInstance can be spawned */
	private int _location;
	private L2Territory _territory;

	/** The maximum number of L2NpcInstance that can manage this L2Spawn */
	private int _maximumCount;

	/** То количество что установлено в базе (текущий максимум может изменяться) */
	private int _referenceCount;

	/** The current number of L2NpcInstance managed by this L2Spawn */
	private int _currentCount;

	/** The current number of SpawnTask in progress or stand by of this L2Spawn */
	protected int _scheduledCount;

	/** The X position of the spawn point */
	private int _locx;

	/** The Y position of the spawn point */
	private int _locy;

	/** The Z position of the spawn point */
	private int _locz;

	/** The heading of L2NpcInstance when they are spawned */
	private int _heading;

	private int _randomx;
	private int _randomy;

	/** The delay between a L2NpcInstance remove and its re-spawn */
	private int _respawnDelay;

	private int _respawnRandom;

	/** Время респауна, unixtime в секундах */
	private int _respawnTime;

	private StatsSet _ai_params = null;
	@SuppressWarnings("unchecked")
	private Constructor<?> _ai_constructor = null;

	/** The generic constructor of L2NpcInstance managed by this L2Spawn */
	@SuppressWarnings("unchecked")
	private Constructor<?> _constructor;

	/** If True a L2NpcInstance is respawned each time that another is killed */
	boolean _doRespawn;

	private L2NpcInstance _lastSpawn;
	private static final List<SpawnListener> _spawnListeners = new ArrayList<SpawnListener>();

	private HashSet<L2NpcInstance> _spawned;

	private int _siegeId;

	private int _reflection = 0;
	private Instance _instance;
	private L2GroupSpawn _groupSpawn;
	private String _eventName;

	/** The task launching the function doSpawn() */
	class SpawnTask implements Runnable
	{
		L2NpcInstance oldNpc;

		public SpawnTask(L2NpcInstance oldNpc)
		{
			this.oldNpc = oldNpc;
		}

		public void run()
		{
			try
			{
				if(_doRespawn)
					respawnNpc(oldNpc);
			}
			catch(Exception e)
			{
				_log.warn("", e);
				e.printStackTrace();
			}

			_scheduledCount--;
		}
	}

	/**
	 * Constructor of L2Spawn.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * Each L2Spawn owns generic and static properties (ex : RewardExp, RewardSP, AggroRange...).
	 * All of those properties are stored in a different L2NpcTemplate for each type of L2Spawn.
	 * Each template is loaded once in the server cache memory (reduce memory use).
	 * When a new instance of L2Spawn is created, server just create a link between the instance and the template.
	 * When a new instance of L2Spawn is created, server just create a link between the instance and the template.
	 * This link is stored in <B>_template</B><BR><BR>
	 *
	 * Each L2NpcInstance is linked to a L2Spawn that manages its spawn and respawn (delay, location...).
	 * This link is stored in <B>_spawn</B> of the L2NpcInstance<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the _template of the L2Spawn </li>
	 * <li>Calculate the implementationName used to generate the generic constructor of L2NpcInstance managed by this L2Spawn</li>
	 * <li>Create the generic constructor of L2NpcInstance managed by this L2Spawn</li><BR><BR>
	 *
	 * @param mobTemplate The L2NpcTemplate to link to this L2Spawn
	 *
	 */
	public L2Spawn(L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException
	{
		// Set the _template of the L2Spawn
		_template = mobTemplate;

		// The Name of the L2NpcInstance type managed by this L2Spawn
		String implementationName = _template.type; // implementing class name

		// Create the generic constructor of L2NpcInstance managed by this L2Spawn
		try
		{
			_constructor = Class.forName("ru.l2gw.gameserver.model.instances." + implementationName + "Instance").getConstructors()[0];
		}
		catch(ClassNotFoundException e)
		{
			Script script = Scripts.getInstance().getClasses().get("npc.model." + implementationName + "Instance");
			if(script == null)
			{
				_log.warn("Script " + "npc.model." + implementationName + "Instance.java not found or loaded with errors. Npc id: " + _template.npcId + " use L2Npc.");
				_constructor = L2NpcInstance.class.getConstructors()[0];
			}
			else
				_constructor = script.getRawClass().getConstructors()[0];
		}

		if(_constructor == null)
			throw new ClassNotFoundException();

		_spawned = new HashSet<L2NpcInstance>(1);
	}

	@SuppressWarnings("unchecked")
	public void setConstructor(Constructor<?> constr)
	{
		_constructor = constr;
	}

	/**
	 * Return the maximum number of L2NpcInstance that this L2Spawn can manage.<BR><BR>
	 */
	public int getAmount()
	{
		return _maximumCount;
	}

	/**
	 * Return the number of L2NpcInstance that this L2Spawn spawned.<BR><BR>
	 */
	public int getSpawnedCount()
	{
		return _currentCount;
	}

	/**
	 * Return the number of L2NpcInstance that this L2Spawn sheduled.<BR><BR>
	 */
	public int getSheduledCount()
	{
		return _scheduledCount;
	}

	/**
	 * Return the Identifier of this L2Spwan (used as key in the SpawnTable).<BR><BR>
	 */
	public int getId()
	{
		return _id;
	}

	/**
	 * Return the Identifier of the location area where L2NpcInstance can be spwaned.<BR><BR>
	 */
	public int getLocation()
	{
		return _location;
	}

	/**
	 * Return the position of the spawn point.<BR><BR>
	 */
	public Location getLoc()
	{
		return new Location(_locx, _locy, _locz);
	}

	/**
	 * Return the X position of the spawn point.<BR><BR>
	 */
	public int getLocx()
	{
		return _locx;
	}

	/**
	 * Return the Y position of the spawn point.<BR><BR>
	 */
	public int getLocy()
	{
		return _locy;
	}

	/**
	 * Return the Z position of the spawn point.<BR><BR>
	 */
	public int getLocz()
	{
		return _locz;
	}

	/**
	 * Return the Identifier of the L2NpcInstance manage by this L2Spwan contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getNpcId()
	{
		return _template.npcId;
	}

	/**
	 * Return the heading of L2NpcInstance when they are spawned.<BR><BR>
	 */
	public int getHeading()
	{
		return _heading;
	}

	/**
	 * Return the delay between a L2NpcInstance remove and its re-spawn.<BR><BR>
	 */
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}

	public int getRespawnRandom()
	{
		return _respawnRandom;
	}

	public int getRespawnTime()
	{
		return _respawnTime;
	}

	/**
	 * Set the maximum number of L2NpcInstance that this L2Spawn can manage.<BR><BR>
	 */
	public void setAmount(int amount)
	{
		if(_referenceCount == 0)
			_referenceCount = amount;
		_maximumCount = amount;
	}

	/**
	 * Восстанавливает измененное количество
	 */
	public void restoreAmount()
	{
		_maximumCount = _referenceCount;
	}

	/**
	 * Set the Identifier of this L2Spwan (used as key in the SpawnTable).<BR><BR>
	 */
	public void setId(int id)
	{
		_id = id;
	}

	/**
	 * Set the Identifier of the location area where L2NpcInstance can be spawned.<BR><BR>
	 */
	public void setLocation(int location)
	{
		_location = location;
	}

	/**
	 * Set the position(x, y, z, heading) of the spawn point.
	 * @param loc Location
	 */
	public void setLoc(Location loc)
	{
		_locx = loc.getX();
		_locy = loc.getY();
		_locz = loc.getZ();
		_heading = loc.getHeading();
	}

	/**
	 * Set the X position of the spawn point.<BR><BR>
	 */
	public void setLocx(int locx)
	{
		_locx = locx;
	}

	/**
	 * Set the Y position of the spawn point.<BR><BR>
	 */
	public void setLocy(int locy)
	{
		_locy = locy;
	}

	/**
	 * Set the Z position of the spawn point.<BR><BR>
	 */
	public void setLocz(int locz)
	{
		_locz = locz;
	}

	/**
	 * Set the heading of L2NpcInstance when they are spawned.<BR><BR>
	 */
	public void setHeading(int heading)
	{
		_heading = heading;
	}

	public void setRandomX(int x)
	{
		_randomx = x;
	}

	public void setRandomY(int y)
	{
		_randomy = y;
	}

	public void setAIParameters(String params)
	{
		if(params != null && !params.isEmpty())
		{
			for(String param : params.split(";"))
				if(!param.isEmpty())
				{
					if(_ai_params == null)
						_ai_params = new StatsSet();
					_ai_params.set(param.split("=")[0], param.split("=")[1]);
				}
		}
	}

	public void setAIType(String type)
	{
		if(type != null && !type.isEmpty())
			try
			{
				if(!type.equalsIgnoreCase("npc"))
					_ai_constructor = Class.forName("ru.l2gw.gameserver.ai." + type).getConstructors()[0];
			}
			catch(Exception e)
			{
				try
				{
					_ai_constructor = Scripts.getInstance().getClasses().get("ai." + type).getRawClass().getConstructors()[0];
				}
				catch(Exception e1)
				{
					_log.warn("L2Spawn AI type " + type + " not found!");
					e1.printStackTrace();
				}
			}
	}

	/**
	 * Decrease the current number of L2NpcInstance of this L2Spawn and if necessary create a SpawnTask to launch after the respawn Delay.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Decrease the current number of L2NpcInstance of this L2Spawn </li>
	 * <li>Check if respawn is possible to prevent multiple respawning caused by lag </li>
	 * <li>Update the current number of SpawnTask in progress or stand by of this L2Spawn </li>
	 * <li>Create a new SpawnTask to launch after the respawn Delay </li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : A respawn is possible ONLY if _doRespawn=True and _scheduledCount + _currentCount < _maximumCount</B></FONT><BR><BR>
	 *
	 */
	public void decreaseCount(L2NpcInstance oldNpc)
	{
		// Decrease the current number of L2NpcInstance of this L2Spawn
		_currentCount--;

		if(_currentCount < 0)
			_currentCount = 0;

		if(_currentCount == 0 && _groupSpawn != null)
			_groupSpawn.decreaseCount();

		// Check if respawn is possible to prevent multiple respawning caused by lag
		if(_doRespawn && _scheduledCount + _currentCount < _maximumCount && _respawnDelay > 0)
		{
			// Update the current number of SpawnTask in progress or stand by of this L2Spawn
			_scheduledCount++;

			// Create a new SpawnTask to launch after the respawn Delay
			ThreadPoolManager.getInstance().scheduleGeneral(new SpawnTask(oldNpc), (_respawnRandom > 0 ? Rnd.get(_respawnRandom) + _respawnDelay : _respawnDelay) * 1000L);
			updateInDb();
		}
		else
			oldNpc.deleteMe();
	}

	/**
	 * Create the initial spawning and set _doRespawn to True.<BR><BR>
	 *
	 * @return The number of L2NpcInstance that were spawned
	 */
	public int init()
	{
		while(_currentCount + _scheduledCount < _maximumCount)
			if(doSpawn(false) == null)
				break;

		_doRespawn = true;

		return _currentCount;
	}

	/**
	 * Create a L2NpcInstance in this L2Spawn.<BR><BR>
	 */
	public L2NpcInstance spawnOne()
	{
		return doSpawn(false);
	}

	public void despawnAll()
	{
		stopRespawn();
		for(L2NpcInstance npc : getAllSpawned())
			if(npc != null)
				npc.deleteMe();
		_currentCount = 0;
	}

	/**
	 * Set _doRespawn to False to stop respawn in this L2Spawn.<BR><BR>
	 */
	public void stopRespawn()
	{
		_doRespawn = false;
	}

	/**
	 * Set _doRespawn to True to start or restart respawn in this L2Spawn.<BR><BR>
	 */
	public void startRespawn()
	{
		_doRespawn = true;
	}

	/**
	 * Create the L2NpcInstance, add it to the world and lauch its onSpawn action.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
	 * The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR><BR>
	 *
	 * <B><U> Actions for an random spawn into location area</U> : <I>(if Locx=0 and Locy=0)</I></B><BR><BR>
	 * <li>Get L2NpcInstance Init parameters and its generate an Identifier </li>
	 * <li>Call the constructor of the L2NpcInstance </li>
	 * <li>Calculate the random position in the location area (if Locx=0 and Locy=0) or get its exact position from the L2Spawn </li>
	 * <li>Set the position of the L2NpcInstance </li>
	 * <li>Set the HP and MP of the L2NpcInstance to the max </li>
	 * <li>Set the heading of the L2NpcInstance (random heading if not defined : value=-1) </li>
	 * <li>Link the L2NpcInstance to this L2Spawn </li>
	 * <li>Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world </li>
	 * <li>Lauch the action onSpawn fo the L2NpcInstance </li><BR><BR>
	 * <li>Increase the current number of L2NpcInstance managed by this L2Spawn  </li><BR><BR>
	 *
	 */
	public L2NpcInstance doSpawn(boolean spawn)
	{
		try
		{
			// Check if the L2Spawn is not a L2Pet or L2Minion spawn
			if(_template.type.equalsIgnoreCase("L2Pet") || _template.type.equalsIgnoreCase("L2Minion"))
			{
				_currentCount++;
				return null;
			}

			// Call the constructor of the L2NpcInstance
			// (can be a L2ArtefactInstance, L2FriendlyMobInstance, L2GuardInstance, L2MonsterInstance, L2SiegeGuardInstance, L2BoxInstance or L2NpcInstance)
			Object tmp = _constructor.newInstance(IdFactory.getInstance().getNextId(), _template, 0L, 0L, 0L, 0L);

			// Check if the Instance is a L2NpcInstance
			if(!(tmp instanceof L2NpcInstance))
				return null;

			if(_ai_constructor != null)
				((L2NpcInstance) tmp).setAIConstructor(_ai_constructor);

			if(_ai_params != null)
				((L2NpcInstance) tmp).setAIParams(_ai_params);

			if(!spawn)
				spawn = _respawnTime <= System.currentTimeMillis() / 1000 + MIN_RESPAWN_DELAY;

			_spawned.add((L2NpcInstance) tmp);

			return intializeNpc((L2NpcInstance) tmp, spawn);
		}
		catch(Exception e)
		{
			_log.warn("NPC " + _template.npcId + " can't spawn" + e.getMessage());
			e.printStackTrace();
		}

		return null;
	}

	public HashSet<L2NpcInstance> getAllSpawned()
	{
		return _spawned;
	}

	private L2NpcInstance intializeNpc(L2NpcInstance mob, boolean spawn) throws NullPointerException
	{
		Location newLoc;
		int newHeading;

		// If Locx=0 and Locy=0, the L2NpcInstance must be spawned in an area defined by location
		if(_territory != null)
		{
			int p[] = _territory.getRandomPoint(mob.isFlying());
			newLoc = new Location(p[0], p[1], p[2]);
			newHeading = Rnd.get(0xFFFF);
		}
		else if(getLocation() != 0)
		{
			// Set the calculated position of the L2NpcInstance
			if(_locx == 0 && _locy == 0)
			{
				// Calculate the random position in the location area
				int p[] = TerritoryTable.getInstance().getRandomPoint(getLocation(), mob.isFlying());
				newLoc = new Location(p[0], p[1], p[2]);
			}
			else
				newLoc = getLoc();

			newHeading = getHeading() == -1 ? Rnd.get(0xFFFF) : getHeading();
		}
		else
		{
			// The L2NpcInstance is spawned at the exact position (Lox, Locy, Locz)
			newLoc = getLoc();

			// random x,y
			if(_randomx > 0 || _randomy > 0)
				newLoc.set(newLoc.getX() + Rnd.get(_randomx), newLoc.getY() + Rnd.get(_randomy), newLoc.getZ());

			// random heading if not defined
			newHeading = getHeading() == -1 ? Rnd.get(0xFFFF) : getHeading();
		}

		// Set the HP and MP of the L2NpcInstance to the max
		mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
		mob.setNpcState(0);
		mob.stopAllEffects();
		mob.removeStatsOwner(mob);
		mob.setWeaponEnchant(0);

		// Link the L2NpcInstance to this L2Spawn
		mob.setSpawn(this);

		// Set the heading of the L2NpcInstance (random heading if not defined)
		mob.setHeading(newHeading);

		// save spawned points
		mob.setSpawnedLoc(newLoc);

		if(_reflection != 0)
			mob.setReflection(_reflection);

		if(spawn)
		{
			// Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world as a visible object
			mob.spawnMe(newLoc);

			// Launch the action onSpawn for the L2NpcInstance
			mob.onSpawn();

			L2Spawn.notifyNpcSpawned(mob);

			// Increase the current number of L2NpcInstance managed by this L2Spawn
			_currentCount++;
		}
		else
		{
			mob.setXYZInvisible(newLoc.getX(), newLoc.getY(), newLoc.getZ());

			// Update the current number of SpawnTask in progress or stand by of this L2Spawn
			_scheduledCount++;

			// Create a new SpawnTask to launch after the respawn Delay
			long delay = _respawnTime * 1000L - System.currentTimeMillis();
			ThreadPoolManager.getInstance().scheduleGeneral(new SpawnTask(mob), delay);

			if(Config.DEBUG && delay > 3600000)
				_log.info("Schedule spawn " + mob + " for " + delay / 1000 + " sec.");
		}

		_lastSpawn = mob;

		if(Config.DEBUG)
			_log.debug("spawned Mob ID: " + _template.npcId + " ,at: " + mob.getX() + " x, " + mob.getY() + " y, " + mob.getZ() + " z");

		return mob;
	}

	public static void addSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.add(listener);
		}
	}

	public static void removeSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.remove(listener);
		}
	}

	public static void notifyNpcSpawned(L2NpcInstance npc)
	{
		synchronized (_spawnListeners)
		{
			for(SpawnListener listener : _spawnListeners)
				listener.npcSpawned(npc);
		}
	}

	/**
	 * @param respawnDelay delay in seconds
	 */
	public void setRespawnDelay(int respawnDelay)
	{
		if(respawnDelay < 0)
			_log.warn("respawn delay is negative for spawnId:" + _id);

		if(respawnDelay != 0 && respawnDelay < MIN_RESPAWN_DELAY)
			respawnDelay = MIN_RESPAWN_DELAY;

		_respawnDelay = respawnDelay;
	}

	public void setRespawnRandom(int respawnRandom)
	{
		_respawnRandom = respawnRandom;
	}

	/**
	 * Устанавливает время следующего респауна
	 * @param respawnTime в unixtime
	 */
	public void setRespawnTime(int respawnTime)
	{
		_respawnTime = respawnTime;
	}

	public L2NpcInstance getLastSpawn()
	{
		return _lastSpawn;
	}

	/**
	 * @param oldNpc
	 */
	public void respawnNpc(L2NpcInstance oldNpc)
	{
		oldNpc.refreshID();
		intializeNpc(oldNpc, true);
	}

	public void updateInDb()
	{
		if(_respawnDelay > 3600 || _respawnDelay == 0)
		{
			_respawnTime = (int)(System.currentTimeMillis() / 1000L) + _respawnDelay;

			if(_respawnRandom > 0)
				_respawnTime += Rnd.get(-_respawnRandom, _respawnRandom);

			if(Config.RAID_FORCE_STATUS_UPDATE)
				mysql.set("REPLACE INTO `kill_status` (`spawn_id`, `npc_templateid`, `current_hp`, `current_mp`, `respawn_time`) VALUES (" + getId() + ", " + getNpcId() +  ", 0, 0, " + _respawnTime + ")");
		}
	}

	public void setSiegeId(int id)
	{
		_siegeId = id;
	}

	public int getSiegeId()
	{
		return _siegeId;
	}

	public void setReflection(int reflection)
	{
		_reflection = reflection;
	}

	public void setTerritory(L2Territory territory)
	{
		_territory = territory;
	}

	public void setInstance(Instance instance)
	{
		_instance = instance;
	}

	public Instance getInstance()
	{
		return _instance;
	}

	public void setGroupSpawn(L2GroupSpawn group)
	{
		_groupSpawn = group;
		if(group != null)
			_eventName = group.getEventName();
	}

	public L2GroupSpawn getGroupSpawn()
	{
		return _groupSpawn;
	}

	public L2Spawn copy()
	{
		L2Spawn spawn = null;
		try
		{
			spawn = new L2Spawn(_template);
			spawn.setId(_id);
			spawn.setAmount(_maximumCount);
			spawn.setLocx(_locx);
			spawn.setLocy(_locy);
			spawn.setLocz(_locz);
			spawn.setHeading(_heading);
			spawn.setRandomX(_randomx);
			spawn.setRandomY(_randomy);
			if(_ai_params != null)
				spawn._ai_params = _ai_params.clone();
			spawn._ai_constructor = _ai_constructor;
			spawn.setRespawnDelay(_respawnDelay);
			spawn.setRespawnRandom(_respawnRandom);
			spawn.setRespawnTime(_respawnTime);
			spawn.setLocation(_location);
			spawn.setEventName(_eventName);
		}
		catch(Exception e)
		{
			_log.warn("L2Spawn: can't copy spawn: " + e);
			e.printStackTrace();
		}
		return spawn;
	}

	public L2NpcTemplate getNpcTemplate()
	{
		return _template;
	}

	public void setEventName(String name)
	{
		_eventName = name;
	}

	public String getEventName()
	{
		return _eventName;
	}

	public int getSpawnedNpcInMyEvent()
	{
		if(_eventName == null)
			return _currentCount;

		GArray<L2Spawn> spawns = SpawnTable.getInstance().getEventSpawns(_eventName);
		if(spawns != null)
		{
			int c = 0;
			for(L2Spawn spawn : spawns)
				c += spawn.getSpawnedCount();

			return c;
		}

		return _currentCount;
	}
}
