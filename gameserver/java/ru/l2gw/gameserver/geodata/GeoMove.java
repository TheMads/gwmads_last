package ru.l2gw.gameserver.geodata;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExShowTrace;
import ru.l2gw.util.Location;

import java.util.ArrayList;


/**
 * @Author: Diamond
 * @Date: 27/04/2009
 */
public class GeoMove
{
	private static final ArrayList<Location> emptyTargetRecorder = new ArrayList<Location>(0);
	private static final ArrayList<ArrayList<Location>> emptyMovePath = new ArrayList<ArrayList<Location>>(0);

	public static ArrayList<Location> findPath(int x, int y, int z, Location target, L2Object obj, boolean showTrace, int refIndex)
	{
		if(Math.abs(z - target.getZ()) > 256)
			return emptyTargetRecorder;

		z = GeoEngine.getHeight(x, y, z, refIndex);
		target.setZ(GeoEngine.getHeight(target, refIndex));

		PathFind n = new PathFind(x, y, z, target.getX(), target.getY(), target.getZ(), refIndex);

		if(n.getPath() == null || n.getPath().isEmpty())
			return emptyTargetRecorder;

		ArrayList<Location> targetRecorder = new ArrayList<Location>(n.getPath().size() + 2);

		// добавляем первую точку в список (начальная позиция чара)
		targetRecorder.add(new Location(x, y, z));

		for(PathFind.GeoNode p : n.getPath())
			targetRecorder.add(p.location.geo2world());

		// добавляем последнюю точку в список (цель)
		targetRecorder.add(target);

		if(Config.PATH_CLEAN)
			pathClean(targetRecorder, refIndex, obj);

		if(showTrace && obj.isPlayer() || obj.isPlayer() && ((L2Player) obj).getVarB("trace"))
		{
			L2Player player = (L2Player) obj;
			ExShowTrace trace = new ExShowTrace();
			int i = 0;
			for(Location loc : targetRecorder)
			{
				i++;
				if(i == 1 || i == targetRecorder.size())
					continue;
				trace.addTrace(loc.getX(), loc.getY(), loc.getZ(), 15000);
			}
			player.sendPacket(trace);
		}

		return targetRecorder;
	}

	public static ArrayList<ArrayList<Location>> findMovePath(int x, int y, int z, Location target, L2Object obj, boolean showTrace, int refIndex)
	{
		return getNodePath(findPath(x, y, z, target, obj, showTrace, refIndex), refIndex);
	}

	public static ArrayList<ArrayList<Location>> getNodePath(ArrayList<Location> path, int refIndex)
	{
		int size = path.size();
		if(size <= 1)
			return emptyMovePath;
		ArrayList<ArrayList<Location>> result = new ArrayList<ArrayList<Location>>();
		for(int i = 1; i < size; i++)
		{
			Location p2 = path.get(i);
			Location p1 = path.get(i - 1);
			ArrayList<Location> moveList = GeoEngine.MoveList(p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), refIndex, true); // onlyFullPath = true - проверяем весь путь до конца
			if(moveList == null) // если хотя-бы через один из участков нельзя пройти, забраковываем весь путь 
				return emptyMovePath;
			if(!moveList.isEmpty()) // это может случиться только если 2 одинаковых точки подряд
				result.add(moveList);
		}
		return result;
	}

	public static ArrayList<Location> constructMoveList(Location begin, Location end)
	{
		begin.world2geo();
		end.world2geo();

		ArrayList<Location> result = new ArrayList<Location>();

		boolean geoZ = Math.abs(end.getZ() - begin.getZ()) > 16;
		int diff_x = end.getX() - begin.getX(), diff_y = end.getY() - begin.getY(), diff_z = geoZ ? (end.getZ() >> 4) - (begin.getZ() >> 4) : end.getZ() - begin.getZ();
		int dx = Math.abs(diff_x), dy = Math.abs(diff_y), dz = Math.abs(diff_z);
		float steps = Math.max(Math.max(dx, dy), dz);
		if(steps == 0) // Никуда не идем
			return result;

		float step_x = diff_x / steps, step_y = diff_y / steps, step_z = diff_z / steps;
		float next_x = begin.getX(), next_y = begin.getY(), next_z = geoZ ? (begin.getZ() >> 4) : begin.getZ();

		result.add(new Location(begin.getX(), begin.getY(), begin.getZ())); // Первая точка

		for(int i = 0; i < steps; i++)
		{
			next_x += step_x;
			next_y += step_y;
			next_z += step_z;

			result.add(new Location(Math.round(next_x), Math.round(next_y), geoZ ? Math.round(next_z) << 4 : Math.round(next_z)));
		}

		return result;
	}

	/**
	 * Очищает путь от ненужных точек.
	 * @param path путь который следует очистить
	 */
	private static void pathClean(ArrayList<Location> path, int refIndex, L2Object obj)
	{
		int size = path.size();

		if(size > 2)
			for(int i = 2; i < size; i++)
			{
				Location p3 = path.get(i); // точка конца движения
				Location p2 = path.get(i - 1); // точка в середине, кандидат на вышибание
				Location p1 = path.get(i - 2); // точка начала движения
				if(p1.equals(p2) || p3.equals(p2) || IsPointInLine(p1, p2, p3)) // если вторая точка совпадает с первой/третьей или на одной линии с ними - она не нужна
				{
					path.remove(i - 1); // удаляем ее
					size--; // отмечаем это в размере массива
					i = Math.max(2, i - 2); // сдвигаемся назад, FIXME: может я тут не совсем прав
				}
			}

		for(int current = 0; current < path.size() - 2; current++)
		{
			for(int sub = current + 2; sub < path.size(); sub++)
			{
				Location one = path.get(current);
				Location two = path.get(sub);
				boolean canMove = GeoEngine.canMoveWithCollision(one.getX(), one.getY(), one.getZ(), two.getX(), two.getY(), two.getZ(), refIndex);

				if(!one.equals(two) && !canMove)
					continue;

				for(; current + 1 < sub; sub--)
					path.remove(current + 1);
			}
		}
	}

	private static boolean IsPointInLine(Location p1, Location p2, Location p3)
	{
		// Все 3 точки на одной из осей X или Y.
		if(p1.getX() == p3.getX() && p3.getX() == p2.getX() || p1.getY() == p3.getY() && p3.getY() == p2.getY())
			return true;
		// Условие ниже выполнится если все 3 точки выстроены по диагонали.
		// Это работает потому, что сравниваем мы соседние точки (расстояния между ними равны, важен только знак).
		// Для случая с произвольными точками работать не будет.
		return (p1.getX() - p2.getX()) * (p1.getY() - p2.getY()) == (p2.getX() - p3.getX()) * (p2.getY() - p3.getY());
	}
}
