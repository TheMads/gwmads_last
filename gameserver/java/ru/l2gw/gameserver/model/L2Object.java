package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.listeners.MethodInvokeListener;
import ru.l2gw.extensions.listeners.PropertyChangeListener;
import ru.l2gw.extensions.listeners.engine.DefaultListenerEngine;
import ru.l2gw.extensions.listeners.engine.ListenerEngine;
import ru.l2gw.extensions.listeners.engine.MethodInvocationResult;
import ru.l2gw.extensions.listeners.events.MethodEvent;
import ru.l2gw.extensions.listeners.events.PropertyEvent;
import ru.l2gw.extensions.scripts.Events;
import ru.l2gw.extensions.scripts.Script;
import ru.l2gw.extensions.scripts.ScriptObject;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.ai.L2CharacterAI;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.entity.vehicle.L2AirShip;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.model.instances.*;
import ru.l2gw.gameserver.serverpackets.StopMove;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.ReflectionTable;
import ru.l2gw.gameserver.tables.TerritoryTable;
import ru.l2gw.util.Location;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class L2Object
{
	private static final Log _log = LogFactory.getLog(L2Object.class.getName());

	protected int _reflection = 0;

	private final boolean isPlayer;
	private final boolean isPlayable;
	private final boolean isPet;
	private final boolean isSummon;
	private final boolean isCubic;
	private final boolean isMonster;
	private final boolean isNpc;
	private final boolean isCharacter;
	private final boolean isItem;
	/** Raid Boss */
	private final boolean isRaid;
	private final boolean isBoss;
	private final boolean isTrap;
	private final boolean isVehicle;
	private final boolean isAirShip;
	/** Object identifier */
	protected int _objectId;
	protected long _storedId;

	/** Object location : Used for items/chars that are seen in the world */
	private int _x;
	private int _y;
	private int _z;

	protected int _l;

	private int _polyid;
	private String _polytype;
	private float _polyRadius;
	private float _polyHeight;

	/** Object visibility */
	protected boolean _hidden;

	//public ReentrantLock region_lock = new ReentrantLock();

	/**
	 * Constructor of L2Object.<BR><BR>
	 * @param objectId этого объекта
	 */
	public L2Object(Integer objectId)
	{
		_objectId = objectId;

		isCharacter = this instanceof L2Character;
		isPlayer = this instanceof L2Player;
		isPlayable = this instanceof L2Playable;
		isPet = this instanceof L2PetInstance;
		isSummon = this instanceof L2SummonInstance;
		isCubic = this instanceof L2CubicInstance;
		isMonster = this instanceof L2MonsterInstance;
		isNpc = this instanceof L2NpcInstance;
		isRaid = this instanceof L2RaidBossInstance || this instanceof L2BossInstance;
		isBoss = this instanceof L2BossInstance;
		isTrap = this instanceof L2TrapInstance;
		isItem = this instanceof L2ItemInstance;
		isAirShip = this instanceof L2AirShip;
		isVehicle = this instanceof L2Vehicle;

		_storedId = this instanceof L2ItemInstance || this instanceof L2CubicInstance ? L2ObjectsStorage.putDummy(this) : L2ObjectsStorage.put(this);
	}

	public Object callScripts(Script scriptClass, Method method, Object[] args)
	{
		return callScripts(scriptClass, method, args, null);
	}

	public Object callScripts(Script scriptClass, Method method, Object[] args, HashMap<String, Object> variables)
	{
		if(ru.l2gw.extensions.scripts.Scripts.loading)
			return null;

		ScriptObject o;
		try
		{
			o = scriptClass.newInstance();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}

		if(variables != null && variables.size() > 0)
			for(Map.Entry<String, Object> obj : variables.entrySet())
				try
				{
					o.setProperty(obj.getKey(), obj.getValue());
				}
				catch(Exception e)
				{}

		try
		{
			o.setProperty("self", this);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		Object ret = args == null ? o.invokeMethod(method) : o.invokeMethod(method, args);

		try
		{
			o.setProperty("self", null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return ret;
	}

	public Object callScripts(String _class, String method, Object[] args)
	{
		return callScripts(_class, method, args, null);
	}

	public Object callScripts(String _class, String method, Object[] args, HashMap<String, Object> variables)
	{
		if(ru.l2gw.extensions.scripts.Scripts.loading)
			return null;

		ScriptObject o;

		Script scriptClass = Scripts.getInstance().getClasses().get(_class);

		if(scriptClass == null)
		{
			_log.info("Script class " + _class + " not found");
			return null;
		}

		try
		{
			o = scriptClass.newInstance();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}

		if(variables != null && variables.size() > 0)
			for(Map.Entry<String, Object> obj : variables.entrySet())
				try
				{
					o.setProperty(obj.getKey(), obj.getValue());
				}
				catch(Exception e)
				{}

		try
		{
			o.setProperty("self", this);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		Object ret = args == null ? o.invokeMethod(method) : o.invokeMethod(method, args);

		try
		{
			o.setProperty("self", null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return ret;
	}

	public int getReflection()
	{
		return _reflection;
	}

	public void setReflection(int i) throws NullPointerException
	{
		if(_reflection != i)
		{
			boolean spawn = false;
			if(!_hidden)
			{
				decayMe();
				spawn = true;
			}

			if(_reflection != 0)
			{
				Reflection ref = ReflectionTable.getInstance().getById(_reflection);
				if(ref != null)
					ref.removeObject(_objectId);
			}

			_reflection = i;
			
			if(i != 0)
			{
				Reflection ref = ReflectionTable.getInstance().getById(i);
				if(ref == null)
					throw new NullPointerException("Try to spawn in not existent reflection: " + i);

				ref.addObject(this);

				if(isPlayer())
				{
					Instance inst = InstanceManager.getInstance().getInstanceByPlayer(getPlayer());
					if(inst != null)
						inst.onPlayerEnter(getPlayer());
				}
			}
			else if(isPlayer())
			{
				Instance inst = InstanceManager.getInstance().getInstanceByPlayer(getPlayer());
				if(inst != null)
					inst.onPlayerExit(getPlayer());
			}

			if(spawn)
				spawnMe();
		}
	}

	/**
	 * @return the identifier of the L2Object.<BR><BR>
	 */
	public final int getObjectId()
	{
		return _objectId;
	}

	public final long getStoredId()
	{
		return _storedId;
	}

	/**
	 * @return the x position of the L2Object.<BR><BR>
	 */
	public int getX()
	{
		return _x;
	}

	/**
	 * @return the y position of the L2Object.<BR><BR>
	 */
	public int getY()
	{
		return _y;
	}

	/**
	 * @return the z position of the L2Object.<BR><BR>
	 */
	public int getZ()
	{
		return _z;
	}

	/**
	 * Возвращает позицию (x, y, z, heading)
	 * @return Location
	 */
	public Location getLoc()
	{
		return new Location(_x, _y, _z, getHeading());
	}

	public int getGeoZ(Location loc)
	{
		if(isPlayer)
		{
			L2Player player = (L2Player) this;
			if(player.isFlying() || player.isSwimming() || player.isInBoat())
				return loc.getZ();
			return GeoEngine.getHeight(loc, getReflection());
		}
		else if(isNpc)
		{
			L2Spawn spawn = ((L2NpcInstance) this).getSpawn();
			if(spawn != null && spawn.getLocx() == 0 && spawn.getLocy() == 0 && !isFlying())
				return GeoEngine.getHeight(loc, getReflection());
			return loc.getZ();
		}
		else if(this instanceof L2Vehicle)
			return loc.getZ();
		return GeoEngine.getHeight(loc, getReflection());
	}

	public void setPolyInfo(String polytype, int polyid)
	{
		_polytype = polytype;
		_polyid = polyid;
		_polyRadius = _polyid > 0 ? NpcTable.getTemplate(_polyid).collisionRadius : 0;
		_polyHeight = _polyid > 0 ? NpcTable.getTemplate(_polyid).collisionHeight : 0;
	}

	public void setPolyInfo(String polytype, String polyid)
	{
		_polytype = polytype;
		_polyid = Integer.parseInt(polyid);
		_polyRadius = _polyid > 0 ? NpcTable.getTemplate(_polyid).collisionRadius : 0;
		_polyHeight = _polyid > 0 ? NpcTable.getTemplate(_polyid).collisionHeight : 0;
	}

	/**
	 * Set the x,y,z position of the L2Object and if necessary modify its _worldRegion.<BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Update position during and after movement, or after teleport </li><BR>
	 *
	 * @param x new x coord
	 * @param y new y coord
	 * @param z new z coord
	 */
	public void setXYZ(int x, int y, int z, boolean move)
	{
		if(!L2World.validCoords(x, y) && !isVehicle)
			if(isPlayer && !getPlayer().isInBoat())
			{
				_log.warn("Player " + this + " (" + _objectId + ") at bad coords: (" + getX() + ", " + getY() + ").");
				L2Player player = (L2Player) this;
				player.abortAttack();
				player.abortCast();
				player.sendActionFailed();
				player.broadcastPacket(new StopMove((L2Player) this));
				player.teleToClosestTown();
				return;
			}
			else if(this instanceof L2NpcInstance)
			{
				L2Spawn spawn = ((L2NpcInstance) this).getSpawn();
				if(spawn == null)
					return;
				if(spawn.getLocx() != 0)
				{
					x = spawn.getLocx();
					y = spawn.getLocy();
					z = spawn.getLocz();
				}
				else
				{
					int p[] = TerritoryTable.getInstance().getRandomPoint(spawn.getLocation(), isFlying());
					x = p[0];
					y = p[1];
					z = p[2];
				}
			}
			else if(isCharacter && !getPlayer().isInBoat() && !isVehicle)
			{
				decayMe();
				return;
			}

		_x = x;
		_y = y;
		_z = z;
		if(L2World.validCoords(x, y))
			revalidateZones(!move);
		L2World.addVisibleObject(this, null);
	}

	/**
	 * Set the x,y,z position of the L2Object and make it invisible.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * A L2Object is invisble if <B>_hidden</B> = true<BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Create a Door</li>
	 * <li> Restore L2Player</li><BR>
	 *
	 * @param x new x coord
	 * @param y new y coord
	 * @param z new z coord
	 */
	public void setXYZInvisible(int x, int y, int z)
	{
		if(x > L2World.MAP_MAX_X)
			x = L2World.MAP_MAX_X - 5000;
		if(x < L2World.MAP_MIN_X)
			x = L2World.MAP_MIN_X + 5000;
		if(y > L2World.MAP_MAX_Y)
			y = L2World.MAP_MAX_Y - 5000;
		if(y < L2World.MAP_MIN_Y)
			y = L2World.MAP_MIN_Y + 5000;

		if(z < -16000 || z > 16000)
			z = 16000;

		_x = x;
		_y = y;
		_z = z;

		//revalidateZones(true);
		_hidden = true;
	}

	public boolean isPolymorphed()
	{
		return _polytype != null;
	}

	public String getPolytype()
	{
		return _polytype;
	}

	public int getPolyid()
	{
		return _polyid;
	}

	/**
	 * Return the visibility state of the L2Object. <BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * A L2Object is invisible if <B>_hidden</B>=true or <B>_worldregion</B>==null <BR><BR>
	 *
	 * @return true if visible
	 */
	public final boolean isVisible()
	{
		return !_hidden;
	}

	public final void spawnMe(Location loc)
	{
		if(loc.getX() > L2World.MAP_MAX_X)
			loc.setX(L2World.MAP_MAX_X - 5000);
		if(loc.getX() < L2World.MAP_MIN_X)
			loc.setX(L2World.MAP_MIN_X + 5000);
		if(loc.getY() > L2World.MAP_MAX_Y)
			loc.setY(L2World.MAP_MAX_Y - 5000);
		if(loc.getY() < L2World.MAP_MIN_Y)
			loc.setY(L2World.MAP_MIN_Y + 5000);

		_x = loc.getX();
		_y = loc.getY();
		_z = getGeoZ(loc);

		if(!isItem() && !isCubic() && L2ObjectsStorage.get(_storedId) == null)
			_storedId = L2ObjectsStorage.put(this);

		spawnMe();
	}

	/**
	 * Добавляет обьект в мир, добавляет в текущий регион. Делает обьект видимым.
	 */
	public void spawnMe()
	{
		// Set the x,y,z position of the L2Object spawn and update its _worldregion
		_hidden = false;

		// Add the L2Oject spawn in the _allobjects of L2World
		L2World.addVisibleObject(this, null);
		revalidateZones(true);
	}

	/**
	 * Do Nothing.<BR><BR>
	 *
	 * <B><U> Overriden in </U> :</B><BR><BR>
	 * <li> L2Summon :  Reset isShowSpawnAnimation flag</li>
	 * <li> L2NpcInstance    :  Reset some flags</li><BR><BR>
	 *
	 */
	public void onSpawn()
	{}

	/**
	 * Удаляет обьект из текущего региона, делая его невидимым.
	 */
	public void decayMe()
	{
		_hidden = true;
		L2World.removeVisibleObject(this);
	}

	public void deleteMe()
	{
		if(!_hidden)
			decayMe();
		L2World.removeObject(this);
		L2ObjectsStorage.remove(_storedId);
	}

	public void onAction(L2Player player, boolean dontMove)
	{
		if(!dontMove && Events.onAction(player, this))
			return;
		else if(dontMove && Events.onActionShift(player, this))
			return;

		player.sendActionFailed();
	}

	public void onForcedAttack(L2Player player, boolean dontMove)
	{
		player.sendActionFailed();
	}

	public abstract boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage);

	public String getL2ClassShortName()
	{
		return getClass().getName().replaceAll("^.*\\.(.*?)$", "$1");
	}

	public String getName()
	{
		return getClass().getSimpleName() + ":" + _objectId;
	}

	public final double getDistance(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	public final double getDistance(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Проверяет в досягаемости расстояния ли объект
	 * @param obj проверяемый объект
	 * @param range расстояние
	 * @return true, если объект досягаем
	 */
	public boolean isInRange(L2Object obj, int range)
	{
		if(obj == null)
			return false;

		if(getReflection() != obj.getReflection())
			return false;

		long dx = Math.abs(obj.getX() - getX());
		if(dx > range)
			return false;
		long dy = Math.abs(obj.getY() - getY());
		if(dy > range)
			return false;
		long dz = Math.abs(obj.getZ() - getZ());
		return dz <= 1500 && dx * dx + dy * dy <= range * range;
	}

	public final boolean isInRangeZ(L2Object obj, int range)
	{
		if(obj == null)
			return false;
		long dx = Math.abs(obj.getX() - getX());
		if(dx > range)
			return false;
		long dy = Math.abs(obj.getY() - getY());
		if(dy > range)
			return false;
		long dz = Math.abs(obj.getZ() - getZ());
		return dz <= range && dx * dx + dy * dy + dz * dz <= range * range;
	}

	public final boolean isInRange(Location loc, long range)
	{
		return (long)(loc.getX() - getX()) * (long)(loc.getX() - getX()) + (long)(loc.getY() - getY()) * (long)(loc.getY() - getY()) <= range * range;
	}

	public final boolean isInRangeSq(Location loc, long range)
	{
		return (long)(loc.getX() - getX()) * (long)(loc.getX() - getX()) + (long)(loc.getY() - getY()) * (long)(loc.getY() - getY()) <= range;
	}

	public final boolean isInRangeSq(L2Object obj, long range)
	{
		return (long)(obj.getX() - getX()) * (long)(obj.getX() - getX()) + (long)(obj.getY() - getY()) * (long)(obj.getY() - getY()) <= range;
	}

	public final boolean isInRangeZ(Location loc, long range)
	{
		return (long)(loc.getX() - getX()) * (long)(loc.getX() - getX()) + (long)(loc.getY() - getY()) * (long)(loc.getY() - getY()) + (long)(loc.getZ() - getZ()) * (long)(loc.getZ() - getZ()) <= range * range;
	}

	public double getDistance(L2Object obj)
	{
		if(obj == null)
			return 0;
		double dx = obj.getX() - getX();
		double dy = obj.getY() - getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	public final double getRealDistance(L2Object obj)
	{
		if(obj == null)
			return 0;
		double dx = obj.getX() - getX();
		double dy = obj.getY() - getY();
		double distance = Math.sqrt(dx * dx + dy * dy);
		if(isCharacter)
			distance -= ((L2Character) this).getTemplate().collisionRadius;
		if(obj.isCharacter())
			distance -= ((L2Character) obj).getTemplate().collisionRadius;
		return distance;
	}

	public final double getRealDistance3D(L2Object obj)
	{
		if(obj == null)
			return 0;
		double dx = obj.getX() - getX();
		double dy = obj.getY() - getY();
		double dz = obj.getZ() - getZ();
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if(isCharacter)
			distance -= ((L2Character) this).getTemplate().collisionRadius;
		if(obj.isCharacter())
			distance -= ((L2Character) obj).getTemplate().collisionRadius;
		return distance;
	}

	public final double getDistance3D(L2Object obj)
	{
		if(obj == null)
			return 0;
		double dx = obj.getX() - getX();
		double dy = obj.getY() - getY();
		double dz = obj.getZ() - getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Возвращает L2Player управляющий даным обьектом.<BR>
	 * <li>Для L2Player это сам игрок.</li>
	 * <li>Для L2Summon это его хозяин.</li><BR><BR>
	 * @return L2Player управляющий даным обьектом.
	 */
	public L2Player getPlayer()
	{
		return null;
	}

	/**
	 * Проверяет наличие объекта в мире
	 * @return true если оъект есть в мире
	 */
	public boolean isInWorld()
	{
		return L2ObjectsStorage.findObject(_objectId) != null;
	}

	public int getHeading()
	{
		return 0;
	}

	
	public float getMoveSpeed()
	{
		return 0;
	}

	private L2WorldRegion _currentRegion = null;

	public L2WorldRegion getCurrentRegion()
	{
		return _currentRegion;
	}

	public void setCurrentRegion(L2WorldRegion region)
	{
		_currentRegion = region;
	}

	public L2CharacterAI getAI()
	{
		return null;
	}

	public boolean hasAI()
	{
		return false;
	}

	public boolean inObserverMode()
	{
		return false;
	}

	public int getOlympiadGameId()
	{
		return -1;
	}

	public void startAttackStanceTask()
	{}

	public float getColRadius()
	{
		_log.warn("getColRadius called directly from L2Object");
		Thread.dumpStack();
		return 0;
	}

	public float getColHeight()
	{
		_log.warn("getColHeight called directly from L2Object");
		Thread.dumpStack();
		return 0;
	}

	public boolean isFlying()
	{
		return false;
	}

	// --------------------------- Listeners system test -----------------------------
	private DefaultListenerEngine<L2Object> listenerEngine;

	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		getListenerEngine().addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		getListenerEngine().removePropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String value, PropertyChangeListener listener)
	{
		getListenerEngine().addPropertyChangeListener(value, listener);
	}

	public void removePropertyChangeListener(String value, PropertyChangeListener listener)
	{
		getListenerEngine().removePropertyChangeListener(value, listener);
	}

	public void firePropertyChanged(String value, Object oldValue, Object newValue)
	{
		getListenerEngine().firePropertyChanged(value, this, oldValue, newValue);
	}

	public void firePropertyChanged(PropertyEvent event)
	{
		getListenerEngine().firePropertyChanged(event);
	}

	public void addProperty(String property, Object value)
	{
		getListenerEngine().addProperty(property, value);
	}

	public Object getProperty(String property)
	{
		return getListenerEngine().getProperty(property);
	}

	public void addMethodInvokeListener(MethodInvokeListener listener)
	{
		getListenerEngine().addMethodInvokedListener(listener);
	}

	public void addMethodInvokeListener(String methodName, MethodInvokeListener listener)
	{
		getListenerEngine().addMethodInvokedListener(methodName, listener);
	}

	public void removeMethodInvokeListener(MethodInvokeListener listener)
	{
		getListenerEngine().removeMethodInvokedListener(listener);
	}

	public void removeMethodInvokeListener(String methodName, MethodInvokeListener listener)
	{
		getListenerEngine().removeMethodInvokedListener(methodName, listener);
	}

	public MethodInvocationResult fireMethodInvoked(MethodEvent event)
	{
		return getListenerEngine().fireMethodInvoked(event);
	}

	public MethodInvocationResult fireMethodInvoked(String methodName, Object[] args)
	{
		return getListenerEngine().fireMethodInvoked(methodName, this, args);
	}

	public ListenerEngine<L2Object> getListenerEngine()
	{
		if(listenerEngine == null)
			listenerEngine = new DefaultListenerEngine<>(this);
		return listenerEngine;
	}

	// ------------------------- Listeners system test end ---------------------------

	public boolean isCharacter()
	{
		return isCharacter;
	}

	public boolean isPlayable()
	{
		return isPlayable;
	}

	public boolean isPlayer()
	{
		return isPlayer;
	}

	public boolean isPet()
	{
		return isPet;
	}

	public boolean isSummon()
	{
		return isSummon;
	}

	public boolean isCubic()
	{
		return isCubic;
	}

	public boolean isMonster()
	{
		return isMonster;
	}

	public boolean isNpc()
	{
		return isNpc;
	}

	public boolean isItem()
	{
		return isItem;
	}

	public boolean isRaid()
	{
		return isRaid;
	}

	public boolean isBoss()
	{
		return isBoss;
	}

	public boolean isTrap()
	{
		return isTrap;
	}

	public boolean isDoor()
	{
		return this instanceof L2DoorInstance;
	}

	public boolean isArtefact()
	{
		return this instanceof L2ArtefactInstance;
	}

	public boolean isSiegeGuard()
	{
		return this instanceof L2SiegeGuardInstance;
	}

	public boolean isVehicle()
	{
		return isVehicle;
	}

	public boolean isAirShip()
	{
		return isAirShip;
	}

	public boolean isMinion()
	{
		return isNpc && ((L2NpcInstance) this).getLeader() != null && !isRaid();
	}

	public void revalidateZones(boolean force)
	{
	}
}
