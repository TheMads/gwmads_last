package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.DayNightSpawnManager;
import ru.l2gw.gameserver.instancemanager.RaidBossSpawnManager;
import ru.l2gw.gameserver.instancemanager.ServerVariables;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2TrapInstance;
import ru.l2gw.util.Location;

import java.util.Arrays;
import java.util.List;

public class L2World
{
	public static final Log _log = LogFactory.getLog(L2World.class.getName());

	/**
	 * Map dimensions
	 */
	public static final int MAP_MIN_X = Config.GEO_X_FIRST - 20 << 15;
	public static final int MAP_MAX_X = (Config.GEO_X_LAST - 19 << 15) - 1;
	public static final int MAP_MIN_Y = Config.GEO_Y_FIRST - 18 << 15;
	public static final int MAP_MAX_Y = (Config.GEO_Y_LAST - 17 << 15) - 1;
	public static final int MAP_MIN_Z = -16384;
	public static final int MAP_MAX_Z = 16383;

	public static final int WORLD_SIZE_X = Config.GEO_X_LAST - Config.GEO_X_FIRST + 1;
	public static final int WORLD_SIZE_Y = Config.GEO_Y_LAST - Config.GEO_Y_FIRST + 1;

	public static final int SHIFT_BY = Config.SHIFT_BY;
	public static final int SHIFT_BY_FOR_Z = Config.SHIFT_BY_FOR_Z;

	/**
	 * calculated offset used so top left region is 0,0
	 */
	public static final int OFFSET_X = Math.abs(MAP_MIN_X >> SHIFT_BY);
	public static final int OFFSET_Y = Math.abs(MAP_MIN_Y >> SHIFT_BY);
	public static final int OFFSET_Z = Math.abs(MAP_MIN_Z >> SHIFT_BY_FOR_Z);

	/**
	 * Размерность массива регионов
	 */
	private static final int REGIONS_X = (MAP_MAX_X >> SHIFT_BY) + OFFSET_X;
	private static final int REGIONS_Y = (MAP_MAX_Y >> SHIFT_BY) + OFFSET_Y;
	private static final int REGIONS_Z = (MAP_MAX_Z >> SHIFT_BY_FOR_Z) + OFFSET_Z;

	/**
	 * Размеры одного игрового региона *
	 */
	private static final int REGION_SIZE_X = (MAP_MAX_X - MAP_MIN_X) / (REGIONS_X + 1) + 1;
	private static final int REGION_SIZE_Y = (MAP_MAX_Y - MAP_MIN_Y) / (REGIONS_Y + 1) + 1;
	private static final int REGION_SIZE_Z = (MAP_MAX_Z - MAP_MIN_Z) / (REGIONS_Z + 1) + 1;

	/* database statistics */
	private static long _insertItemCounter = 0;
	private static long _deleteItemCounter = 0;
	private static long _updateItemCounter = 0;
	private static long _lazyUpdateItem = 0;
	private static long _updatePlayerBase = 0;

	private static L2WorldRegion[][][] _worldRegions = new L2WorldRegion[REGIONS_X + 1][REGIONS_Y + 1][];

	private static long _taxSum;

	private static L2World _instance = new L2World();

	static
	{
		List<String> split_regions = Arrays.asList(Config.VERTICAL_SPLIT_REGIONS.split(";"));

		for(int x = 0; x <= REGIONS_X; x++)
			for(int y = 0; y <= REGIONS_Y; y++)
			{
				int wx = ((((x - OFFSET_X) << SHIFT_BY) - MAP_MIN_X) >> 15) + Config.GEO_X_FIRST;
				int wy = ((((y - OFFSET_Y) << SHIFT_BY) - MAP_MIN_Y) >> 15) + Config.GEO_Y_FIRST;
				if(split_regions.contains(wx + "_" + wy))
					_worldRegions[x][y] = new L2WorldRegion[REGIONS_Z + 1];
				else
					_worldRegions[x][y] = new L2WorldRegion[1];
			}
	}

	public static L2World getInstance()
	{
		return _instance;
	}

	/**
	 * Выдает массив регионов, примыкающих к текущему, плюс текущий регион (куб 3х3х3)
	 * На входе - координаты региона (уже преобразованные)
	 * @param  regX
	 * @param  regY
	 * @param  regZ
	 * @param  deepH
	 * @param  deepV
	 * @return neighbors
	 */
	public static GArray<L2WorldRegion> getNeighbors(int regX, int regY, int regZ, int deepH, int deepV)
	{
		GArray<L2WorldRegion> neighbors = new GArray<L2WorldRegion>();

		deepH *= 2;
		deepV *= 2;
		int rx, ry, rz;
		for(int x = 0; x <= deepH; x++)
			for(int y = 0; y <= deepH; y++)
				for(int z = 0; z <= deepV; z++)
				{
					rx = regX + (x % 2 == 0 ? -x / 2 : x - x / 2);
					ry = regY + (y % 2 == 0 ? -y / 2 : y - y / 2);
					rz = 0;
					if(validRegion(rx, ry, rz))
					{
						if(_worldRegions[rx][ry].length > 1)
						{
							rz = regZ + (z % 2 == 0 ? -z / 2 : z - z / 2);
							if(!validRegion(rx, ry, rz))
								continue;
						}
						else
							z = deepV + 1;

						if(_worldRegions[rx][ry][rz] != null)
							neighbors.add(_worldRegions[rx][ry][rz]);
					}
				}
		return neighbors;
	}

	/**
	 * @param x координата
	 * @param y координата
	 * @param z координата
	 * @return Правильные ли координаты для региона
	 */
	private static boolean validRegion(int x, int y, int z)
	{
		return x >= 0 && x < REGIONS_X && y >= 0 && y < REGIONS_Y && z >= 0 && z < REGIONS_Z;
	}

	/**
	 * @param obj обьект для посика региона
	 * @return Регион, соответствующий координатам обьекта (не путать с L2Object.getCurrentRegion())
	 */
	public static L2WorldRegion getRegion(L2Object obj)
	{
		return getRegion(obj.getX(), obj.getY(), obj.getZ());
	}

	/**
	 * @param x координата
	 * @param y координата
	 * @param z координата
	 * @return Регион, соответствующий координатам
	 */
	public static L2WorldRegion getRegion(int x, int y, int z)
	{
		int _x = (x >> SHIFT_BY) + OFFSET_X;
		int _y = (y >> SHIFT_BY) + OFFSET_Y;
		int _z = 0;
		if(validRegion(_x, _y, _z))
		{
			if(_worldRegions[_x][_y].length > 1)
				_z = (z >> SHIFT_BY_FOR_Z) + OFFSET_Z;

			if(_worldRegions[_x][_y][_z] == null)
				_worldRegions[_x][_y][_z] = new L2WorldRegion(_x, _y, _z);
			return _worldRegions[_x][_y][_z];
		}
		return null;
	}

	public static int getActiveRegionsCount()
	{
		int ret = 0;
		for(L2WorldRegion[][] wr0 : _worldRegions)
			if(wr0 != null)
				for(L2WorldRegion[] wr1 : wr0)
					if(wr1 != null)
						for(L2WorldRegion wr2 : wr1)
							if(wr2 != null && wr2.isActive())
								ret++;
		return ret;
	}

	/**
	 * Удаляет обьект из _allObjects (а так же из _allCharacters и _allPlayers для частных случаев)
	 * Также делается попытка удалить игков из всех территорий.
	 *
	 * @param object Обьект для удаления
	 */
	public static void removeObject(L2Object object)
	{
		if(object == null)
			return;

		// Удаление обьекта из всех территорий, также очистка территорий у удаленного обьекта
		if(object.isCharacter())
			((L2Character) object).clearZones();
	}

	/**
	 * Проверяет, сменился ли регион в котором находится обьект
	 * Если сменился - удаляет обьект из старого региона и добавляет в новый.
	 *
	 * @param object  обьект для проверки
	 * @param dropper - если это L2ItemInstance, то будет анимация дропа с перса
	 */
	public static void addVisibleObject(L2Object object, L2Character dropper)
	{
		if(object == null || !object.isVisible() || (object.inObserverMode() && object.getOlympiadGameId() < 0))
			return;

		if(object.isPet() || object.isSummon())
		{
			L2Player owner = object.getPlayer();
			if(owner != null && object.getReflection() != owner.getReflection())
				object.setReflection(owner.getReflection());
		}
		else if(object.isTrap()) // FIXME увеличиваем костыль далее
		{
			L2Character owner = ((L2TrapInstance) object).getTrapOwner();
			if(owner != null && object.getReflection() != owner.getReflection())
				object.setReflection(owner.getReflection());
		}

		L2WorldRegion region = null;
		L2WorldRegion currentRegion = null;
		//object.region_lock.lock();
		try
		{
			region = getRegion(object);
			currentRegion = object.getCurrentRegion();

			if(region == null || currentRegion != null && currentRegion.equals(region))
			{
				if(object instanceof L2DoorInstance)
					_log.warn("L2World: addVisibleObject: " + object + " ref: " + object.getReflection() + " reg: " + region + (region == null ? " " + object.getLoc() : "") + " curReg: " + currentRegion);
				return;
			}

			// Добавляем обьект в список видимых, т.к. если сначала удалить, и регион опустеет - произойдет
			// перезапуск AI у всех мобах в новом реионе и всех соседях, что в свою очередь удалит все эффекты,
			// почистит аггролисты, и сделает много прочей гадости.
			region.addObject(object);
			object.setCurrentRegion(region);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		//finally
		//{
		//	object.region_lock.unlock();
		//}
		// Убираем из старых регионов обьект
		if(currentRegion == null) // Новый обьект (пример - игрок вошел в мир, заспаунился моб, дропнули вещь)
		{
			// Показываем обьект в текущем и соседних регионах
			// Если обьект игрок, показываем ему все обьекты в текущем и соседних регионах
			GArray<L2WorldRegion> newNeighbors = region.getNeighbors();
			for(L2WorldRegion neighbor : newNeighbors)
				if(neighbor != null)
					neighbor.addToPlayers(object, dropper);
		}
		else
		// Обьект уже существует, перешел из одного региона в другой
		{
			// Показываем обьект, но в отличие от первого случая - только для новых соседей.
			// Убираем обьект из старых соседей.
			//long time = System.currentTimeMillis();
			//int c = 0;

			GArray<L2WorldRegion> oldNeighbors = currentRegion.getNeighbors();
			GArray<L2WorldRegion> newNeighbors = region.getNeighbors();
			try
			{
				for(L2WorldRegion neighbor : currentRegion.getNeighbors())
				{
					boolean flag = true;
					for(L2WorldRegion newneighbor : newNeighbors)
					{
						//c++;
						if(newneighbor.equals(neighbor))
						{
							flag = false;
							break;
						}
					}

					if(flag)
						neighbor.removeFromPlayers(object);
				}

				for(L2WorldRegion neighbor : newNeighbors)
				{
					boolean flag = true;
					for(L2WorldRegion oldneighbor : oldNeighbors)
					{
						//c++;
						if(oldneighbor.equals(neighbor))
						{
							flag = false;
							break;
						}
					}

					if(flag)
						neighbor.addToPlayers(object, dropper);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				currentRegion.removeObject(object, true);
			}

			//time = System.currentTimeMillis() - time;
			//if(object.isPlayer() && object.getPlayer().isGM())
			//	object.getPlayer().sendMessage("Region changed: " + currentRegion.getRegStr() + " -> " + region.getRegStr() + " " + time + " ms " + c);
		}
	}

	public static void sendObjectsToPlayer(L2Player player)
	{
		L2WorldRegion currentRegion = player.getCurrentRegion();
		if(currentRegion != null)
		{
			GArray<L2WorldRegion> neighbors = currentRegion.getNeighbors();
			for(L2WorldRegion region : neighbors)
				if(region != null)
					for(L2Object obj : region.getObjectsList(player.getReflection()))
					{
						if(obj == null)
							continue;

						player.addVisibleObject(obj, null);
					}
		}
	}

	/**
	 * Удаляет обьект из текущего региона
	 *
	 * @param object обьект для удаления
	 */
	public static void removeVisibleObject(L2Object object)
	{
		if(object == null || object.isVisible() || (object.inObserverMode() && object.getOlympiadGameId() < 0))
			return;
		L2WorldRegion region = object.getCurrentRegion();
		if(region != null)
		{
			region.removeObject(object, false);
			for(L2WorldRegion neighbor : region.getNeighbors())
				if(neighbor != null)
					neighbor.removeFromPlayers(object);
			object.setCurrentRegion(null);
		}
	}

	/**
	 * Проверяет координаты на корректность
	 *
	 * @param x координата x
	 * @param y координата y
	 * @return Корректные ли координаты
	 */
	public static boolean validCoords(int x, int y)
	{
		return x > MAP_MIN_X && x < MAP_MAX_X && y > MAP_MIN_Y && y < MAP_MAX_Y;
	}

	/**
	 * Удаляет весь спаун
	 */
	public static synchronized void deleteVisibleNpcSpawns()
	{
		RaidBossSpawnManager.getInstance().cleanUp();
		DayNightSpawnManager.getInstance().cleanUp();
		_log.info("Deleting all visible NPC's...");
		for(int i = 0; i < REGIONS_X; i++)
			for(int j = 0; j < REGIONS_Y; j++)
				for(int k = 0; k < _worldRegions[i][j].length; k++)
					if(_worldRegions[i][j][k] != null)
						_worldRegions[i][j][k].deleteVisibleNpcSpawns();
		_log.info("All visible NPC's deleted.");
	}

	public static L2Object getAroundObjectById(L2Object object, Integer id)
	{
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return null;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			if(region != null && region.getObjectsSize() > 0)
				for(L2Object o : region.getObjectsList(object.getReflection()))
					if(o != null && o.getObjectId() == id)
						return o;
		return null;
	}

	public static GArray<L2Object> getAroundObjects(L2Object object)
	{
		int oid = object.getObjectId();
		GArray<L2Object> result = new GArray<L2Object>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			if(region != null && region.getObjectsSize() > 0)
				for(L2Object o : region.getObjectsList(object.getReflection()))
				{
					if(o == null || o.getObjectId() == oid)
						continue;
					if(o.inObserverMode() && o.getOlympiadGameId() < 0 && region.equals(o.getCurrentRegion()))
						continue;

					result.add(o);
				}
		return result;
	}

	public static GArray<L2Object> getAroundObjects(L2Object object, int radius, int height)
	{
		int x = object.getX();
		int y = object.getY();
		int z = object.getZ();
		long sqrad = radius * radius;
		GArray<L2Object> result = new GArray<L2Object>();
		int oid = object.getObjectId();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			if(region != null && region.getObjectsSize() > 0)
				for(L2Object o : region.getObjectsList(object.getReflection()))
				{
					if(o == null || o.getObjectId() == oid)
						continue;
					if(Math.abs(o.getZ() - z) > height)
						continue;
					long dx = o.getX() - x;
					dx *= dx;
					if(dx > sqrad)
						continue;
					if(o.inObserverMode() && o.getOlympiadGameId() < 0 && region.equals(o.getCurrentRegion()))
						continue;

					long dy = o.getY() - y;
					dy *= dy;
					if(dx + dy < sqrad)
						result.add(o);
				}
		return result;
	}

	public static GArray<L2Character> getAroundCharacters(L2Object object)
	{
		int oid = object.getObjectId();
		GArray<L2Character> result = new GArray<L2Character>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			if(region != null && region.getObjectsSize() > 0)
				for(L2Character o : region.getCharactersList(object.getReflection()))
				{
					if(o.getObjectId() == oid)
						continue;
					if(o.inObserverMode() && o.getOlympiadGameId() < 0 && region.equals(o.getCurrentRegion()))
						continue;

					result.add(o);
				}
		return result;
	}

	public static GArray<L2Character> getAroundCharacters(L2Object object, int radius, int height)
	{
		int x = object.getX();
		int y = object.getY();
		int z = object.getZ();
		long sqrad = radius * radius;
		GArray<L2Character> result = new GArray<L2Character>();
		int oid = object.getObjectId();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			if(region != null && region.getObjectsSize() > 0)
				for(L2Character o : region.getCharactersList(object.getReflection()))
				{
					if(o.getObjectId() == oid)
						continue;
					if(Math.abs(o.getZ() - z) > height)
						continue;
					long dx = o.getX() - x;
					dx *= dx;
					if(dx > sqrad)
						continue;
					long dy = o.getY() - y;
					dy *= dy;
					if(dx + dy < sqrad)
						result.add(o);
				}
		return result;
	}

	public static GArray<L2NpcInstance> getAroundNpc(L2Object object)
	{
		int oid = object.getObjectId();
		GArray<L2NpcInstance> result = new GArray<L2NpcInstance>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			if(region != null && region.getObjectsSize() > 0)
				for(L2NpcInstance o : region.getNpcsList(object.getReflection()))
				{
					if(o.getObjectId() == oid)
						continue;
					result.add(o);
				}
		return result;
	}

	public static GArray<L2NpcInstance> getAroundNpc(L2Object object, int radius, int height)
	{
		int x = object.getX();
		int y = object.getY();
		int z = object.getZ();

		int deepH = 1;
		int deepV = 1;
		if(radius > REGION_SIZE_X)
			deepH = radius / REGION_SIZE_X + 1;
		if(height > REGION_SIZE_Z)
			deepV = height / REGION_SIZE_Z + 1;

		L2WorldRegion currenRegion = L2World.getRegion(x, y, z);
		GArray<L2NpcInstance> result = new GArray<L2NpcInstance>();
		if(currenRegion == null)
		{
			_log.info("No region for: " + x + ", " + y + ", " + z);
			return result;
		}
		long sqrad = radius * radius;
		int oid = object.getObjectId();

		for(L2WorldRegion region : currenRegion.getNeighbors(deepH, deepV))
			if(region != null && region.getObjectsSize() > 0)
				for(L2NpcInstance o : region.getNpcsList(object.getReflection()))
				{
					if(o.getObjectId() == oid)
						continue;
					if(Math.abs(o.getZ() - z) > height)
						continue;
					long dx = o.getX() - x;
					dx *= dx;
					if(dx > sqrad)
						continue;
					long dy = o.getY() - y;
					dy *= dy;
					if(dx + dy < sqrad)
						result.add(o);
				}
		return result;
	}

	public static GArray<L2Playable> getAroundPlayables(L2Object object)
	{
		int oid = object.getObjectId();
		GArray<L2Playable> result = new GArray<L2Playable>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			if(region != null && region.getObjectsSize() > 0)
				for(L2Object o : region.getObjectsList(object.getReflection()))
				{
					if(o == null || !(o instanceof L2Playable) || o.getObjectId() == oid)
						continue;
					if(o.inObserverMode() && o.getOlympiadGameId() < 0 && region.equals(o.getCurrentRegion()))
						continue;

					result.add((L2Playable) o);
				}
		return result;
	}

	public static GArray<L2Playable> getAroundPlayables(L2Object object, int radius, int height)
	{
		int x = object.getX();
		int y = object.getY();
		int z = object.getZ();
		long sqrad = radius * radius;
		GArray<L2Playable> result = new GArray<L2Playable>();
		int oid = object.getObjectId();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			if(region != null && region.getObjectsSize() > 0)
				for(L2Object o : region.getObjectsList(object.getReflection()))
				{
					if(o == null || !(o.isPlayer() || o.isPet() || o.isSummon()) || o.getObjectId() == oid)
						continue;
					L2Playable obj = (L2Playable) o;
					if(Math.abs(obj.getZ() - z) > height)
						continue;
					long dx = obj.getX() - x;
					dx *= dx;
					if(dx > sqrad)
						continue;
					if(o.inObserverMode() && o.getOlympiadGameId() < 0 && region.equals(o.getCurrentRegion()))
						continue;

					long dy = obj.getY() - y;
					dy *= dy;
					if(dx + dy < sqrad)
						result.add(obj);
				}
		return result;
	}

	public static GArray<L2Player> getAroundPlayers(L2Object object)
	{
		int oid = object.getObjectId();
		GArray<L2Player> result = new GArray<L2Player>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			if(region != null && region.getObjectsSize() > 0)
				for(L2Player o : region.getPlayersList(object.getReflection()))
				{
					if(o.getObjectId() == oid)
						continue;
					if(o.inObserverMode() && o.getOlympiadGameId() < 0 && region.equals(o.getCurrentRegion()))
						continue;
					result.add(o);
				}
		return result;
	}

	public static GArray<L2Player> getAroundPlayers(L2Object object, int radius, int height)
	{
		return getAroundPlayers(object.getLoc(), object.getReflection(), object.getObjectId(), radius, height);
	}

	/**
	 * Возвращает список игроков в заданном радиусе и высоте
	 *
	 * @param loc	  - Вокруг какой точки
	 * @param refIndex - reflection
	 * @param objectId - который будет исключен из списка
	 * @param radius   - радиус
	 * @param height   - высота
	 * @return Список игроков
	 */
	public static GArray<L2Player> getAroundPlayers(Location loc, int refIndex, int objectId, int radius, int height)
	{
		int x = loc.getX();
		int y = loc.getY();
		int z = loc.getZ();
		long quad = radius * radius;

		GArray<L2Player> result = new GArray<L2Player>();
		L2WorldRegion currenRegion = L2World.getRegion(loc.getX(), loc.getY(), loc.getZ());

		if(currenRegion == null)
			return result;

		int deepH = 1;
		int deepV = 1;
		if(radius > REGION_SIZE_X)
			deepH = radius / REGION_SIZE_X + 1;
		if(height > REGION_SIZE_Z)
			deepV = height / REGION_SIZE_Z + 1;

		for(L2WorldRegion region : currenRegion.getNeighbors(deepH, deepV))
			if(region != null && region.getObjectsSize() > 0)
				for(L2Player o : region.getPlayersList(refIndex))
				{
					if(o.getObjectId() == objectId)
						continue;
					if(Math.abs(o.getZ() - z) > height)
						continue;
					long dx = o.getX() - x;
					dx *= dx;
					if(dx > quad)
						continue;
					if(o.inObserverMode() && o.getOlympiadGameId() < 0 && region.equals(o.getCurrentRegion()))
						continue;
					long dy = o.getY() - y;
					dy *= dy;
					if(dx + dy < quad)
						result.add(o);
				}
		return result;
	}

	// database statistic methods
	// items

	public static void increaseInsertItemCount()
	{
		_insertItemCounter++;
	}

	public static long getInsertItemCount()
	{
		return _insertItemCounter;
	}

	public static void increaseDeleteItemCount()
	{
		_deleteItemCounter++;
	}

	public static long getDeleteItemCount()
	{
		return _deleteItemCounter;
	}

	public static void increaseUpdateItemCount()
	{
		_updateItemCounter++;
	}

	public static long getUpdateItemCount()
	{
		return _updateItemCounter;
	}

	public static void increaseLazyUpdateItem()
	{
		_lazyUpdateItem++;
	}

	public static long getLazyUpdateItem()
	{
		return _lazyUpdateItem;
	}

	// players

	public static void increaseUpdatePlayerBase()
	{
		_updatePlayerBase++;
	}

	public static long getUpdatePlayerBase()
	{
		return _updatePlayerBase;
	}

	public static void loadTaxSum()
	{
		_taxSum = ServerVariables.getLong("taxsum", 0);
	}

	public static void addTax(long sum)
	{
		_taxSum += sum;
		ServerVariables.set("taxsum", _taxSum);
	}

	public static long getTaxSum()
	{
		return _taxSum;
	}

	public static GArray<L2Vehicle> getAroundTransport(Location loc, int radius, int objectId)
	{
		GArray<L2Vehicle> result = new GArray<L2Vehicle>();
		L2WorldRegion currentRegion = L2World.getRegion(loc.getX(), loc.getY(), loc.getZ());

		if(currentRegion == null)
			return result;

		for(L2WorldRegion region : currentRegion.getNeighbors())
			if(region != null && region.getObjectsSize() > 0)
				for(L2Object o : region.getObjectsList(0))
					if(o != null && o.isVehicle() && o.getObjectId() != objectId && o.isInRange(loc, radius))
						result.add((L2Vehicle) o);

		return result;
	}
}
