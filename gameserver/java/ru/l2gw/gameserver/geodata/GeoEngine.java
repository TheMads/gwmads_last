package ru.l2gw.gameserver.geodata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Territory;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.instances.L2ArtefactInstance;
import ru.l2gw.util.Location;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @Author: Diamond
 * @CoAuthor: DRiN
 * @Date: 01/03/2009
 */
public class GeoEngine
{
	private static final Log log = LogFactory.getLog(GeoEngine.class.getName());

	private static final byte EAST = 1, WEST = 2, SOUTH = 4, NORTH = 8, NSWE_ALL = 15, NSWE_NONE = 0;
	private static final byte BLOCKTYPE_FLAT = 0;
	private static final byte BLOCKTYPE_COMPLEX = 1;
	private static final byte BLOCKTYPE_MULTILEVEL = 2;
	private static final int BlocksInMap = 256 * 256;
	private static int MAX_LAYERS = 1; // меньше 1 быть не должно, что бы создавались временные массивы как минимум short[2]

	private static final int Door_MaxZDiff = 256;

	/**
	 * Даный массив содержит всю геодату на сервере. <BR>
	 * Первые 2 [][] (byte[*][*][][]) являются x и y региона.<BR>
	 * Третий [] (byte[][][*][]) является контейнером для всех блоков в регионе.<BR>
	 * Четвертый [] (byte[][][][*]) является блоком геодаты.<BR>
	 */
	private static final byte[][][][][] geodata = new byte[L2World.WORLD_SIZE_X][L2World.WORLD_SIZE_Y][][][];

	public static short getType(int x, int y, int refIndex)
	{
		return NgetType(x - L2World.MAP_MIN_X >> 4, y - L2World.MAP_MIN_Y >> 4, refIndex);
	}

	public static int getHeight(Location loc, int refIndex)
	{
		return getHeight(loc.getX(), loc.getY(), loc.getZ(), refIndex);
	}

	public static int getHeight(int x, int y, int z, int refIndex)
	{
		return NgetHeight(x - L2World.MAP_MIN_X >> 4, y - L2World.MAP_MIN_Y >> 4, z, refIndex);
	}

	public static boolean canMoveToCoord(int x, int y, int z, int tx, int ty, int tz, int refIndex)
	{
		return canMove(x, y, z, tx, ty, tz, false, refIndex) == 0;
	}

	public static byte getNSWE(int x, int y, int z, int refIndex)
	{
		return NgetNSWE(x - L2World.MAP_MIN_X >> 4, y - L2World.MAP_MIN_Y >> 4, z, refIndex);
	}

	public static Location moveCheck(int x, int y, int z, int tx, int ty, int refIndex)
	{
		return MoveCheck(x, y, z, tx, ty, false, false, false, refIndex);
	}

/*
	public static Location moveCheck(int x, int y, int z, int tx, int ty, boolean returnPrev, int refIndex)
	{
		return MoveCheck(x, y, z, tx, ty, false, false, returnPrev, refIndex);
	}

	public static Location moveCheckWithCollision(int x, int y, int z, int tx, int ty, int refIndex)
	{
		return MoveCheck(x, y, z, tx, ty, true, false, false, refIndex);
	}

	public static Location moveCheckBackward(int x, int y, int z, int tx, int ty, int refIndex)
	{
		return MoveCheck(x, y, z, tx, ty, false, true, false, refIndex);
	}

	public static Location moveCheckBackward(int x, int y, int z, int tx, int ty, boolean returnPrev, int refIndex)
	{
		return MoveCheck(x, y, z, tx, ty, false, true, returnPrev, refIndex);
	}

	public static Location moveCheckBackwardWithCollision(int x, int y, int z, int tx, int ty, int refIndex)
	{
		return MoveCheck(x, y, z, tx, ty, true, true, false, refIndex);
	}

*/
	public static Location moveCheckWithCollision(int x, int y, int z, int tx, int ty, boolean returnPrev, int refIndex)
	{
		return MoveCheck(x, y, z, tx, ty, true, false, returnPrev, refIndex);
	}

	public static Location moveCheckBackwardWithCollision(int x, int y, int z, int tx, int ty, boolean returnPrev, int refIndex)
	{
		return MoveCheck(x, y, z, tx, ty, true, true, returnPrev, refIndex);
	}

	public static ArrayList<Location> moveListInWater(int x, int y, int z, int tx, int ty, int tz, int refIndex)
	{
		return MoveListInWater(x - L2World.MAP_MIN_X >> 4, y - L2World.MAP_MIN_Y >> 4, z, tx - L2World.MAP_MIN_X >> 4, ty - L2World.MAP_MIN_Y >> 4, tz, refIndex);
	}

	public static Location moveCheckForAI(Location loc1, Location loc2, int refIndex)
	{
		return MoveCheckForAI(loc1.getX() - L2World.MAP_MIN_X >> 4, loc1.getY() - L2World.MAP_MIN_Y >> 4, loc1.getZ(), loc2.getX() - L2World.MAP_MIN_X >> 4, loc2.getY() - L2World.MAP_MIN_Y >> 4, refIndex);
	}

/*
	public static Location moveCheckInAir(int x, int y, int z, int tx, int ty, int tz, float ColRadius, int refIndex)
	{
		int gx = x - L2World.MAP_MIN_X >> 4;
		int gy = y - L2World.MAP_MIN_Y >> 4;
		int tgx = tx - L2World.MAP_MIN_X >> 4;
		int tgy = ty - L2World.MAP_MIN_Y >> 4;

		int nz = NgetHeight(tgx, tgy, tz, refIndex);

		// Не даем опуститься ниже, чем пол + 32
		if(tz <= nz + 32)
			tz = nz + 32;

		Location result = canSee(gx, gy, z, tgx, tgy, tz, true, refIndex);
		if(result.equals(gx, gy, z))
			return null;

		return result.geo2world();
	}
*/

	public static boolean canSeeTarget(L2Character actor, L2Object target)
	{
		return canSeeTarget(actor, target, actor.isFloating() || actor.isInAirShip());
	}

	public static boolean canSeeTarget(L2Object actor, L2Object target, boolean air)
	{
		return canSeeTarget(actor, target, air, false);
	}

	public static boolean canSeeTarget(L2Object actor, L2Object target, boolean air, boolean debug)
	{
		if(target == null)
			return false;
		if(target instanceof L2ArtefactInstance)
			return actor.isInRange(target, 120) && Math.abs(actor.getZ() - target.getZ()) < 64;
		// Костыль конечно, но решает кучу проблем с дверьми
		return target instanceof GeoControl || actor.equals(target) || canSeeCoord(actor, target.getX(), target.getY(), target.getZ() + (int) target.getColHeight() + 64/*, actor.isPlayer()*/, air, debug);
	}

	public static boolean canSeeCoord(L2Object actor, int tx, int ty, int tz, boolean air)
	{
		return canSeeCoord(actor, tx, ty, tz, air, false);
	}

	public static boolean canSeeCoord(L2Object actor, int tx, int ty, int tz, boolean air, boolean debug)
	{
		return actor != null && canSeeCoord(actor.getX(), actor.getY(), actor.getZ() + (int) actor.getColHeight() + 64, tx, ty, tz, air, actor.getReflection(), debug);
	}

	public static boolean canSeeCoord(int x, int y, int z, int tx, int ty, int tz, boolean air, int refIndex)
	{
		return canSeeCoord(x, y, z, tx, ty, tz, air, refIndex, false);
	}

	public static boolean canSeeCoord(int x, int y, int z, int tx, int ty, int tz, boolean air, int refIndex, boolean debug)
	{
		int mx = x - L2World.MAP_MIN_X >> 4;
		int my = y - L2World.MAP_MIN_Y >> 4;
		int tmx = tx - L2World.MAP_MIN_X >> 4;
		int tmy = ty - L2World.MAP_MIN_Y >> 4;
		return canSee(mx, my, z, tmx, tmy, tz, air, refIndex, debug).equals(tmx, tmy, tz) && canSee(tmx, tmy, tz, mx, my, z, air, refIndex, debug).equals(mx, my, z);
	}

	public static boolean canMoveWithCollision(int x, int y, int z, int tx, int ty, int tz, int refIndex)
	{
		return canMove(x, y, z, tx, ty, tz, true, refIndex) == 0;
	}

	/**
	 * @param NSWE
	 * @param x
	 * @param y
	 * @param tx
	 * @param ty
	 * @return True if NSWE dont block given direction
	 */
	private static boolean checkNSWE(byte NSWE, int x, int y, int tx, int ty)
	{
		if(NSWE == NSWE_ALL)
			return true;
		if(NSWE == NSWE_NONE)
			return false;
		if(tx > x)
		{
			if((NSWE & EAST) == 0)
				return false;
		}
		else if(tx < x)
			if((NSWE & WEST) == 0)
				return false;
		if(ty > y)
		{
			if((NSWE & SOUTH) == 0)
				return false;
		}
		else if(ty < y)
			if((NSWE & NORTH) == 0)
				return false;
		return true;
	}

	private static short NLOS_WATER(int x, int y, int z, int next_x, int next_y, int next_z, int refIndex)
	{
		Layer[] layers1 = NGetLayers(x, y, refIndex);
		Layer[] layers2 = NGetLayers(next_x, next_y, refIndex);
		if(layers1.length == 0 || layers2.length == 0)
			return Short.MIN_VALUE;

		//System.out.println("NLOS: " + z + " --> " + next_z);
		// Находим ближайший к целевой клетке слой
		short z2 = Short.MIN_VALUE;
		for(Layer layer : layers2)
		{
			//System.out.println("NLOS: next z: " + layer.height);
			if(Math.abs(next_z - z2) > Math.abs(next_z - layer.height))
				z2 = layer.height;
		}

		//System.out.println("NLOS: closest next z: " + z2);
		// Луч проходит над преградой
		if(next_z >= z2)
			return (short) next_z;
		else if(Math.abs(next_z - z2) <= 64)
		{
			//System.out.println("NLOS: return geo z: " + z2);
			return z2;
		}

		// Либо перед нами стена, либо над нами потолок. Ищем слой пониже, для уточнения
		short z3 = Short.MIN_VALUE;
		for(Layer layer : layers2)
			if(layer.height < next_z + Config.MIN_LAYER_HEIGHT && Math.abs(next_z - z3) > Math.abs(next_z - layer.height))
				z3 = layer.height;

		//System.out.println("NLOS: closest lower next z: " + z3);

		// Ниже нет слоев, значит это стена
		if(z3 == Short.MIN_VALUE)
			return Short.MIN_VALUE;

		// Собираем данные о предыдущей клетке, игнорируя верхние слои
		short z1 = Short.MIN_VALUE;
		byte NSWE1 = NSWE_ALL;
		for(Layer layer : layers1)
			if(layer.height < z + Config.MIN_LAYER_HEIGHT && Math.abs(z - z1) > Math.abs(z - layer.height))
			{
				z1 = layer.height;
				NSWE1 = layer.nswe;
			}

		//System.out.println("NLOS: closest z: " + z1);

		// Невозможная ситуация, но все же...
		if(z1 < -30000)
			return (short) next_z;

		// Если есть NSWE, то считаем за стену
		return checkNSWE(NSWE1, x, y, next_x, next_y) ? (short) next_z : Short.MIN_VALUE;
	}

	private static int FindNearestLowerLayer(short[] layers, int z)
	{
		short h, nearest_layer_h = Short.MIN_VALUE;
		int nearest_layer = Integer.MIN_VALUE;
		for(int i = 1; i <= layers[0]; i++)
		{
			h = (short) ((short) (layers[i] & 0x0fff0) >> 1);
			if(h < z && nearest_layer_h < h)
			{
				nearest_layer_h = h;
				nearest_layer = layers[i];
			}
		}
		return nearest_layer;
	}

	private static short CheckNoOneLayerInRangeAndFindNearestLowerLayer(short[] layers, int z0, int z1)
	{
		int z_min, z_max;
		if(z0 > z1)
		{
			z_min = z1;
			z_max = z0;
		}
		else
		{
			z_min = z0;
			z_max = z1;
		}
		short h, nearest_layer = Short.MIN_VALUE, nearest_layer_h = Short.MIN_VALUE;
		for(int i = 1; i <= layers[0]; i++)
		{
			h = (short) ((short) (layers[i] & 0x0fff0) >> 1);
			if(z_min <= h && h <= z_max)
				return Short.MIN_VALUE;
			if(h < z0 && nearest_layer_h < h)
			{
				nearest_layer_h = h;
				nearest_layer = layers[i];
			}
		}
		return nearest_layer;
	}
/*
	public static boolean canSeeWallCheck(Layer layer, Layer nearest_lower_neighbor, byte directionNSWE)
	{
		return (layer.nswe & directionNSWE) != 0 || layer.height <= nearest_lower_neighbor.height || Math.abs(layer.height - nearest_lower_neighbor.height) < Config.MAX_Z_DIFF;
	}
*/

	private static boolean canSeeWallCheck(short layer, short nearest_lower_neighbor, byte directionNSWE, int curr_z, boolean air, boolean debug)
	{
		short nearest_lower_neighborh = (short) ((short) (nearest_lower_neighbor & 0x0fff0) >> 1);
		if(air)
			return nearest_lower_neighborh < curr_z;
		short layerh = (short) ((short) (layer & 0x0fff0) >> 1);
		int zdiff = nearest_lower_neighborh - layerh;
		if(debug)
			log.info("canSeeWallCheck: " + layerh + " zDiff: " + zdiff + " nearest_lower_neighborh: " + nearest_lower_neighborh + " return: " + ((layer & 0x0F & directionNSWE) != 0 || zdiff > -Config.MAX_Z_DIFF && zdiff != 0));
		return (layer & 0x0F & directionNSWE) != 0 || zdiff > -Config.MAX_Z_DIFF && zdiff != 0;
	}

	/**
	 * проверка видимости
	 *
	 * @return возвращает последнюю точку которую видно (в формате геокоординат)
	 *         в результате (Location) h является кодом, если >= 0 то успешно достигли последней точки, если меньше то не последней
	 */
	private static Location canSee(int _x, int _y, int _z, int _tx, int _ty, int _tz, boolean air, int refIndex, boolean debug)
	{
		int diff_x = _tx - _x, diff_y = _ty - _y, diff_z = _tz - _z;
		int dx = Math.abs(diff_x), dy = Math.abs(diff_y);

		float steps = Math.max(dx, dy);
		int curr_x = _x, curr_y = _y, curr_z = _z;
		short[] curr_layers = new short[MAX_LAYERS + 1];
		NGetLayers(curr_x, curr_y, curr_layers, refIndex);

		Location result = new Location(_x, _y, _z, -1);

		if(steps == 0)
		{
			if(CheckNoOneLayerInRangeAndFindNearestLowerLayer(curr_layers, curr_z, curr_z + diff_z) != Short.MIN_VALUE)
				result.set(_tx, _ty, _tz, 1);
			return result;
		}

		float step_x = diff_x / steps, step_y = diff_y / steps, step_z = diff_z / steps;
		int half_step_z = (int) (step_z / 2);
		float next_x = curr_x, next_y = curr_y, next_z = curr_z;
		int i_next_x, i_next_y, i_next_z, middle_z;
		short[] tmp_layers = new short[MAX_LAYERS + 1];
		short src_nearest_lower_layer, dst_nearest_lower_layer, tmp_nearest_lower_layer;

		for(int i = 0; i < steps; i++)
		{
			if(curr_layers[0] == 0)
			{
				result.set(_tx, _ty, _tz, 0);
				return result; // Здесь нет геодаты, разрешаем
			}

			next_x += step_x;
			next_y += step_y;
			next_z += step_z;
			i_next_x = Math.round(next_x);
			i_next_y = Math.round(next_y);
			i_next_z = Math.round(next_z);
			middle_z = curr_z + half_step_z;

			if((src_nearest_lower_layer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(curr_layers, curr_z, middle_z)) == Short.MIN_VALUE)
				return result.setH(-10); // либо есть преграждающая поверхность, либо нет снизу слоя и значит это "пустота", то что за стеной или за колоной

			NGetLayers(curr_x, curr_y, curr_layers, refIndex);
			if(curr_layers[0] == 0)
			{
				result.set(_tx, _ty, _tz, 0);
				return result; // Здесь нет геодаты, разрешаем
			}

			if((dst_nearest_lower_layer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(curr_layers, i_next_z, middle_z)) == Short.MIN_VALUE)
				return result.setH(-11); // либо есть преграда, либо нет снизу слоя и значит это "пустота", то что за стеной или за колоной

			if(curr_x == i_next_x)
			{
				if(debug)
					log.info("canSee: " + curr_x + " " + i_next_y);

				//движемся по вертикали
				if(!canSeeWallCheck(src_nearest_lower_layer, dst_nearest_lower_layer, i_next_y > curr_y ? SOUTH : NORTH, curr_z, air, debug))
					return result.setH(-20);
			}
			else if(curr_y == i_next_y)
			{
				if(debug)
					log.info("canSee: " + i_next_x + " " + curr_y);

				//движемся по горизонтали
				if(!canSeeWallCheck(src_nearest_lower_layer, dst_nearest_lower_layer, i_next_x > curr_x ? EAST : WEST, curr_z, air, debug))
					return result.setH(-21);
			}
			else
			{
				//движемся по диагонали
				NGetLayers(curr_x, i_next_y, tmp_layers, refIndex);
				if(tmp_layers[0] == 0)
				{
					result.set(_tx, _ty, _tz, 0);
					return result; // Здесь нет геодаты, разрешаем
				}
				if((tmp_nearest_lower_layer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(tmp_layers, i_next_z, middle_z)) == Short.MIN_VALUE)
					return result.setH(-30); // либо есть преграда, либо нет снизу слоя и значит это "пустота", то что за стеной или за колоной

				if(!(canSeeWallCheck(src_nearest_lower_layer, tmp_nearest_lower_layer, i_next_y > curr_y ? SOUTH : NORTH, curr_z, air, debug) && canSeeWallCheck(tmp_nearest_lower_layer, dst_nearest_lower_layer, i_next_x > curr_x ? EAST : WEST, curr_z, air, debug)))
				{
					NGetLayers(i_next_x, curr_y, tmp_layers, refIndex);
					if(tmp_layers[0] == 0)
					{
						result.set(_tx, _ty, _tz, 0);
						return result; // Здесь нет геодаты, разрешаем
					}
					if((tmp_nearest_lower_layer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(tmp_layers, i_next_z, middle_z)) == Short.MIN_VALUE)
						return result.setH(-31); // либо есть преграда, либо нет снизу слоя и значит это "пустота", то что за стеной или за колоной
					if(!canSeeWallCheck(src_nearest_lower_layer, tmp_nearest_lower_layer, i_next_x > curr_x ? EAST : WEST, curr_z, air, debug))
						return result.setH(-32);
					if(!canSeeWallCheck(tmp_nearest_lower_layer, dst_nearest_lower_layer, i_next_x > curr_x ? EAST : WEST, curr_z, air, debug))
						return result.setH(-33);
				}
			}

			result.set(curr_x, curr_y, curr_z);
			curr_x = i_next_x;
			curr_y = i_next_y;
			curr_z = i_next_z;
		}

		result.set(_tx, _ty, _tz, 0xFF);
		return result;
	}

	private static ArrayList<Location> MoveListInWater(int geoX, int geoY, int z, int tGeoX, int tGeoY, int tz, int refIndex)
	{
		int diff_x = tGeoX - geoX, diff_y = tGeoY - geoY, diff_z = (tz >> 4) - (z >> 4);
		int dx = Math.abs(diff_x), dy = Math.abs(diff_y), dz = Math.abs(diff_z);
		float steps = Math.max(Math.max(dx, dy), dz);

		if(steps < 2) // Никуда не идем
			return new ArrayList<Location>(0);

		float step_x = diff_x / steps, step_y = diff_y / steps, step_z = diff_z / steps;
		int curr_x = geoX, curr_y = geoY, curr_z = z >> 4;
		float next_x = geoX, next_y = geoY, next_z = z >> 4;
		int i_next_x, i_next_y, i_next_z, p_next_z;

		ArrayList<Location> ret = new ArrayList<Location>();
		ret.add(new Location(curr_x, curr_y, z));

		//System.out.println("MoveListInWater: " + steps + " " + z + " --> " + tz + " step_z: " + String.format("%.2f", step_z));

		for(int i = 1; i < steps; i++)
		{
			next_x += step_x;
			next_y += step_y;
			//System.out.println("MoveListInWater: next_z: " + String.format("%.2f", next_z));
			next_z += step_z;
			i_next_x = Math.round(next_x);
			i_next_y = Math.round(next_y);
			i_next_z = Math.round(next_z);
			//System.out.println("MoveListInWater: next_z: " + String.format("%.2f", next_z) + " " + i_next_z);

			if(curr_x == i_next_x || curr_y == i_next_y) // Движение не по диагонали
			{
				if((p_next_z = NLOS_WATER(curr_x, curr_y, curr_z << 4, i_next_x, i_next_y, i_next_z << 4, refIndex)) == Short.MIN_VALUE)
					return ret;
			}
			else // Движение по диагонали
			{
				if((p_next_z = NLOS_WATER(curr_x, curr_y, curr_z << 4, i_next_x, curr_y < i_next_y ? curr_y + 1 : curr_y - 1, i_next_z << 4, refIndex)) == Short.MIN_VALUE)
					return ret;

				//i_next_z >>= 4;
				if((p_next_z = NLOS_WATER(curr_x, curr_y < i_next_y ? curr_y + 1 : curr_y - 1, curr_z << 4, curr_x < i_next_x ? curr_x + 1 : curr_x - 1, i_next_y, p_next_z, refIndex)) == Short.MIN_VALUE)
					return ret;
			}

			curr_x = i_next_x;
			curr_y = i_next_y;
			curr_z = p_next_z >> 4;
			if(p_next_z < i_next_z)
				next_z = p_next_z >> 4;
			//next_z = curr_z >> 4;

			//System.out.println("MoveListInWater: " + i_next_x + "," + i_next_y + "," + p_next_z);
			ret.add(new Location(curr_x, curr_y, p_next_z));
		}

		return ret;
	}

	/**
	 * проверка проходимости по прямой
	 *
	 * @return 0 - проходимо, в ином случае код причины непроходимости (используется при отладке)
	 */
	private static int canMove(int __x, int __y, int _z, int __tx, int __ty, int _tz, boolean withCollision, int refIndex)
	{
		int _x = __x - L2World.MAP_MIN_X >> 4;
		int _y = __y - L2World.MAP_MIN_Y >> 4;
		int _tx = __tx - L2World.MAP_MIN_X >> 4;
		int _ty = __ty - L2World.MAP_MIN_Y >> 4;
		int diff_x = _tx - _x, diff_y = _ty - _y, diff_z;
		int dx = Math.abs(diff_x), dy = Math.abs(diff_y);
		float steps = Math.max(dx, dy);
		if(steps == 0)
			return -5;

		int curr_x = _x, curr_y = _y, curr_z = _z;
		short[] curr_layers = new short[MAX_LAYERS + 1];
		NGetLayers(curr_x, curr_y, curr_layers, refIndex);
		if(curr_layers[0] == 0)
			return 0;

		float step_x = diff_x / steps, step_y = diff_y / steps;
		float next_x = curr_x, next_y = curr_y;
		int i_next_x, i_next_y;

		short[] next_layers = new short[MAX_LAYERS + 1];
		short[] temp_layers = new short[MAX_LAYERS + 1];
		short[] curr_next_switcher;

		for(int i = 0; i < steps; i++)
		{
			next_x += step_x;
			next_y += step_y;
			i_next_x = Math.round(next_x);
			i_next_y = Math.round(next_y);
			NGetLayers(i_next_x, i_next_y, next_layers, refIndex);
			if((curr_z = NcanMoveNext(curr_x, curr_y, curr_z, curr_layers, i_next_x, i_next_y, next_layers, temp_layers, withCollision, refIndex)) == Integer.MIN_VALUE)
				return 1;
			curr_next_switcher = curr_layers;
			curr_layers = next_layers;
			next_layers = curr_next_switcher;
			curr_x = i_next_x;
			curr_y = i_next_y;
		}
		diff_z = curr_z - _tz;
		return diff_z < Config.MAX_Z_DIFF ? 0 : diff_z * 10000;
	}

	public static Location moveCheckSimple(List<Location> locs, int refIndex)
	{
		for(int i = 0; i < locs.size() - 1; i++)
		{
			Location loc1 = locs.get(i);
			Location loc2 = locs.get(i + 1);
			byte NSWE = NgetNSWE(loc1.getX(), loc1.getY(), loc1.getZ(), refIndex);

			if(loc1.getX() == loc2.getX() || loc1.getY() == loc2.getY())
			{
				if(!checkNSWE(NSWE, loc1.getX(), loc1.getY(), loc2.getX(), loc2.getY()))
					return loc1;
			}
			else
			{
				if(!checkNSWE(NSWE, loc1.getX(), loc1.getY(), loc1.getX(), loc2.getY()))
					return loc1;
				if(!checkNSWE(NSWE, loc1.getX(), loc2.getY(), loc2.getX(), loc2.getY()))
					return loc1;
			}
		}
		return locs.get(locs.size() - 1);
	}

	private static Location MoveCheck(int __x, int __y, int _z, int __tx, int __ty, boolean withCollision, boolean backwardMove, boolean returnPrev, int refIndex)
	{
		int _x = __x - L2World.MAP_MIN_X >> 4;
		int _y = __y - L2World.MAP_MIN_Y >> 4;
		int _tx = __tx - L2World.MAP_MIN_X >> 4;
		int _ty = __ty - L2World.MAP_MIN_Y >> 4;

		int diff_x = _tx - _x, diff_y = _ty - _y;
		int dx = Math.abs(diff_x), dy = Math.abs(diff_y);
		float steps = Math.max(dx, dy);
		if(steps == 0)
			return new Location(__x, __y, _z);

		float step_x = diff_x / steps, step_y = diff_y / steps;
		int curr_x = _x, curr_y = _y, curr_z = _z;
		float next_x = curr_x, next_y = curr_y;
		int i_next_x, i_next_y, i_next_z;

		short[] next_layers = new short[MAX_LAYERS + 1];
		short[] temp_layers = new short[MAX_LAYERS + 1];
		short[] curr_layers = new short[MAX_LAYERS + 1];
		short[] curr_next_switcher;
		NGetLayers(curr_x, curr_y, curr_layers, refIndex);
		int prev_x = curr_x, prev_y = curr_y, prev_z = curr_z;

		for(int i = 0; i < steps; i++)
		{
			next_x += step_x;
			next_y += step_y;
			i_next_x = Math.round(next_x);
			i_next_y = Math.round(next_y);
			NGetLayers(i_next_x, i_next_y, next_layers, refIndex);
			if((i_next_z = NcanMoveNext(curr_x, curr_y, curr_z, curr_layers, i_next_x, i_next_y, next_layers, temp_layers, withCollision, refIndex)) == Integer.MIN_VALUE)
				break;
			if(backwardMove && NcanMoveNext(i_next_x, i_next_y, i_next_z, next_layers, curr_x, curr_y, curr_layers, temp_layers, withCollision, refIndex) == Integer.MIN_VALUE)
				break;
			curr_next_switcher = curr_layers;
			curr_layers = next_layers;
			next_layers = curr_next_switcher;
			if(returnPrev)
			{
				prev_x = curr_x;
				prev_y = curr_y;
				prev_z = curr_z;
			}
			curr_x = i_next_x;
			curr_y = i_next_y;
			curr_z = i_next_z;
		}

		if(returnPrev)
		{
			curr_x = prev_x;
			curr_y = prev_y;
			curr_z = prev_z;
		}

		//if(curr_x == _x && curr_y == _y)
		//	return new Location(__x, __y, _z);

		//log.info("move" + (backwardMove ? " back" : "") + (withCollision ? " +collision" : "") + ": " + curr_x + " " + curr_y + " " + curr_z + " / xyz: " + __x + " " + __y + " " + _z + " / to xy: " + __tx + " " + __ty + " / geo xy: " + _x + " " + _y + " / geo to xy: " + _tx + " " + _ty);
		return new Location(curr_x, curr_y, curr_z).geo2world();
	}

	/**
	 * Аналогичен CanMove, но возвращает весь пройденный путь. В гео координатах.
	 */
	public static ArrayList<Location> MoveList(int __x, int __y, int _z, int __tx, int __ty, int refIndex, boolean onlyFullPath)
	{
		int _x = __x - L2World.MAP_MIN_X >> 4;
		int _y = __y - L2World.MAP_MIN_Y >> 4;
		int _tx = __tx - L2World.MAP_MIN_X >> 4;
		int _ty = __ty - L2World.MAP_MIN_Y >> 4;

		int diff_x = _tx - _x, diff_y = _ty - _y;
		int dx = Math.abs(diff_x), dy = Math.abs(diff_y);
		float steps = Math.max(dx, dy);
		if(steps == 0) // Никуда не идем
			return new ArrayList<Location>(0);

		float step_x = diff_x / steps, step_y = diff_y / steps;
		int curr_x = _x, curr_y = _y, curr_z = _z;
		float next_x = curr_x, next_y = curr_y;
		int i_next_x, i_next_y, i_next_z;

		short[] next_layers = new short[MAX_LAYERS + 1];
		short[] temp_layers = new short[MAX_LAYERS + 1];
		short[] curr_layers = new short[MAX_LAYERS + 1];
		short[] curr_next_switcher;

		NGetLayers(curr_x, curr_y, curr_layers, refIndex);
		if(curr_layers[0] == 0)
			return null;

		ArrayList<Location> result = new ArrayList<Location>();

		result.add(new Location(curr_x, curr_y, curr_z)); // Первая точка

		for(int i = 0; i < steps; i++)
		{
			next_x += step_x;
			next_y += step_y;
			i_next_x = Math.round(next_x);
			i_next_y = Math.round(next_y);

			NGetLayers(i_next_x, i_next_y, next_layers, refIndex);
			if((i_next_z = NcanMoveNext(curr_x, curr_y, curr_z, curr_layers, i_next_x, i_next_y, next_layers, temp_layers, false, refIndex)) == Integer.MIN_VALUE)
				if(onlyFullPath)
					return null;
				else
					break;

			curr_next_switcher = curr_layers;
			curr_layers = next_layers;
			next_layers = curr_next_switcher;

			curr_x = i_next_x;
			curr_y = i_next_y;
			curr_z = i_next_z;

			result.add(new Location(curr_x, curr_y, curr_z));
		}

		return result;
	}

	/**
	 * Используется только для антипаровоза в AI
	 */
	private static Location MoveCheckForAI(int x, int y, int z, int tx, int ty, int refIndex)
	{
		int dx = tx - x;
		int dy = ty - y;
		int inc_x = sign(dx);
		int inc_y = sign(dy);
		dx = Math.abs(dx);
		dy = Math.abs(dy);
		if(dx + dy < 2 || dx == 2 && dy == 0 || dx == 0 && dy == 2)
			return new Location(x, y, z).geo2world();
		int prev_x;
		int prev_y;
		int prev_z;
		int next_x = x;
		int next_y = y;
		int next_z = z;
		if(dx >= dy) // dy/dx <= 1
		{
			int delta_A = 2 * dy;
			int d = delta_A - dx;
			int delta_B = delta_A - 2 * dx;
			for(int i = 0; i < dx; i++)
			{
				prev_x = x;
				prev_y = y;
				prev_z = z;
				x = next_x;
				y = next_y;
				z = next_z;
				if(d > 0)
				{
					d += delta_B;
					next_x += inc_x;
					next_y += inc_y;
				}
				else
				{
					d += delta_A;
					next_x += inc_x;
				}
				next_z = NcanMoveNextForAI(x, y, z, next_x, next_y, refIndex);
				if(next_z == 0)
					return new Location(prev_x, prev_y, prev_z).geo2world();
			}
		}
		else
		{
			int delta_A = 2 * dx;
			int d = delta_A - dy;
			int delta_B = delta_A - 2 * dy;
			for(int i = 0; i < dy; i++)
			{
				prev_x = x;
				prev_y = y;
				prev_z = z;
				x = next_x;
				y = next_y;
				z = next_z;
				if(d > 0)
				{
					d += delta_B;
					next_x += inc_x;
					next_y += inc_y;
				}
				else
				{
					d += delta_A;
					next_y += inc_y;
				}
				next_z = NcanMoveNextForAI(x, y, z, next_x, next_y, refIndex);
				if(next_z == 0)
					return new Location(prev_x, prev_y, prev_z).geo2world();
			}
		}
		return new Location(next_x, next_y, next_z).geo2world();
	}

	private static boolean NcanMoveNextExCheck(int x, int y, int h, int nextx, int nexty, int hexth, short[] temp_layers, int refIndex)
	{
		NGetLayers(x, y, temp_layers, refIndex);
		if(temp_layers[0] == 0)
			return true;

		int temp_layer;
		if((temp_layer = FindNearestLowerLayer(temp_layers, h + Config.MIN_LAYER_HEIGHT)) == Integer.MIN_VALUE)
			return false;
		short temp_layer_h = (short) ((short) (temp_layer & 0x0fff0) >> 1);
		//if(Math.abs(temp_layer_h - hexth) >= Config.MAX_Z_DIFF || Math.abs(temp_layer_h - h) >= Config.MAX_Z_DIFF)
		return !(hexth - temp_layer_h >= Config.MAX_Z_DIFF || temp_layer_h - h >= Config.MAX_Z_DIFF) && checkNSWE((byte) (temp_layer & 0x0F), x, y, nextx, nexty);
	}

	/**
	 * @return возвращает высоту следующего блока, либо Integer.MIN_VALUE если двигатся нельзя
	 */
	private static int NcanMoveNext(int x, int y, int z, short[] layers, int next_x, int next_y, short[] next_layers, short[] temp_layers, boolean withCollision, int refIndex)
	{
		if(layers[0] == 0 || next_layers[0] == 0)
			return z;

		int layer, next_layer;
		if((layer = FindNearestLowerLayer(layers, z + Config.MIN_LAYER_HEIGHT)) == Integer.MIN_VALUE)
			return Integer.MIN_VALUE;

		byte layer_nswe = (byte) (layer & 0x0F);
		if(!checkNSWE(layer_nswe, x, y, next_x, next_y))
			return Integer.MIN_VALUE;

		short layer_h = (short) ((short) (layer & 0x0fff0) >> 1);
		if((next_layer = FindNearestLowerLayer(next_layers, layer_h + Config.MIN_LAYER_HEIGHT)) == Integer.MIN_VALUE)
			return Integer.MIN_VALUE;

		byte next_nswe = (byte) (next_layer & 0x0F);
		if(next_nswe == NSWE_NONE)
			return Integer.MIN_VALUE;

		short next_layer_h = (short) ((short) (next_layer & 0x0fff0) >> 1);
		/*if(withCollision && next_layer_h + Config.MAX_Z_DIFF < layer_h)
			return Integer.MIN_VALUE;*/

		// если движение не по диагонали
		if(x == next_x || y == next_y)
		{
			if(withCollision)
			{
				//short[] heightNSWE = temp_layers;
				if(x == next_x)
				{
					NgetHeightAndNSWE(x - 1, y, layer_h, temp_layers, refIndex);
					if(Math.abs(temp_layers[0] - layer_h) > 15 || !checkNSWE(layer_nswe, x - 1, y, x, y) || !checkNSWE((byte) temp_layers[1], x - 1, y, x - 1, next_y))
						return Integer.MIN_VALUE;

					NgetHeightAndNSWE(x + 1, y, layer_h, temp_layers, refIndex);
					if(Math.abs(temp_layers[0] - layer_h) > 15 || !checkNSWE(layer_nswe, x + 1, y, x, y) || !checkNSWE((byte) temp_layers[1], x + 1, y, x + 1, next_y))
						return Integer.MIN_VALUE;

					return next_layer_h;
				}

				NgetHeightAndNSWE(x, y - 1, layer_h, temp_layers, refIndex);
				//if(Math.abs(temp_layers[0] - layer_h) >= Config.MAX_Z_DIFF || !checkNSWE(layer_nswe, x, y - 1, x, y) || !checkNSWE((byte) temp_layers[1], x, y - 1, next_x, y - 1))
				if(temp_layers[0] - layer_h >= Config.MAX_Z_DIFF || !checkNSWE(layer_nswe, x, y - 1, x, y) || !checkNSWE((byte) temp_layers[1], x, y - 1, next_x, y - 1))
					return Integer.MIN_VALUE;

				NgetHeightAndNSWE(x, y + 1, layer_h, temp_layers, refIndex);
				//if(Math.abs(temp_layers[0] - layer_h) >= Config.MAX_Z_DIFF || !checkNSWE(layer_nswe, x, y + 1, x, y) || !checkNSWE((byte) temp_layers[1], x, y + 1, next_x, y + 1))
				if(temp_layers[0] - layer_h >= Config.MAX_Z_DIFF || !checkNSWE(layer_nswe, x, y + 1, x, y) || !checkNSWE((byte) temp_layers[1], x, y + 1, next_x, y + 1))
					return Integer.MIN_VALUE;
			}

			return next_layer_h;
		}

		if(!NcanMoveNextExCheck(x, next_y, layer_h, next_x, next_y, next_layer_h, temp_layers, refIndex))
			return Integer.MIN_VALUE;
		if(!NcanMoveNextExCheck(next_x, y, layer_h, next_x, next_y, next_layer_h, temp_layers, refIndex))
			return Integer.MIN_VALUE;

		//FIXME if(withCollision)

		return next_layer_h;
	}

	/**
	 * Используется только для антипаровоза в AI
	 */
	private static int NcanMoveNextForAI(int x, int y, int z, int next_x, int next_y, int refIndex)
	{
		Layer[] layers1 = NGetLayers(x, y, refIndex);
		Layer[] layers2 = NGetLayers(next_x, next_y, refIndex);

		if(layers1.length == 0 || layers2.length == 0)
			return z == 0 ? 1 : z;

		short z1 = Short.MIN_VALUE;
		short z2 = Short.MIN_VALUE;
		byte NSWE1 = NSWE_ALL;
		byte NSWE2 = NSWE_ALL;

		for(Layer layer : layers1)
			if(layer.height < z + Config.MIN_LAYER_HEIGHT && Math.abs(z - z1) > Math.abs(z - layer.height))
			{
				z1 = layer.height;
				NSWE1 = layer.nswe;
			}

		// Вторая попытка с более мягкими условиями
		if(z1 < -30000)
			for(Layer layer : layers1)
				if(Math.abs(z - z1) > Math.abs(z - layer.height))
				{
					z1 = layer.height;
					NSWE1 = layer.nswe;
				}

		if(z1 < -30000)
			return 0;

		for(Layer layer : layers2)
			if(layer.height < z1 + Config.MIN_LAYER_HEIGHT && Math.abs(z1 - z2) > Math.abs(z1 - layer.height))
			{
				z2 = layer.height;
				NSWE2 = layer.nswe;
			}

		// Вторая попытка с более мягкими условиями
		if(z2 < -30000)
			for(Layer layer : layers2)
				if(Math.abs(z1 - z2) > Math.abs(z1 - layer.height))
				{
					z2 = layer.height;
					NSWE2 = layer.nswe;
				}

		if(z2 < -30000)
			return 0;

		if(z1 > z2 && z1 - z2 > Config.MAX_Z_DIFF)
			return 0;

		if(!checkNSWE(NSWE1, x, y, next_x, next_y) || !checkNSWE(NSWE2, next_x, next_y, x, y))
			return 0;

		return z2 == 0 ? 1 : z2;
	}

	/**
	 * в нулевую ячейку кладется длина
	 *
	 * @param geoX
	 * @param geoY
	 * @param result
	 */
	private static void NGetLayers(int geoX, int geoY, short[] result, int refIndex)
	{
		result[0] = 0;
		byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, refIndex);
		if(block == null)
			return;

		int cellX, cellY;
		int index = 0;
		// Read current block type: 0 - flat, 1 - complex, 2 - multilevel
		byte type = block[index];
		index++;

		switch(type)
		{
			case BLOCKTYPE_FLAT:
				short height = makeShort(block[index + 1], block[index]);
				height = (short) (height & 0x0fff0);
				result[0]++;
				result[1] = (short) ((short) (height << 1) | NSWE_ALL);
				return;
			case BLOCKTYPE_COMPLEX:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				index += (cellX << 3) + cellY << 1;
				height = makeShort(block[index + 1], block[index]);
				result[0]++;
				result[1] = height;
				return;
			case BLOCKTYPE_MULTILEVEL:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = block[index];
					index += (lc << 1) + 1;
					offset--;
				}
				byte layer_count = block[index];
				index++;
				if(layer_count <= 0 || layer_count > MAX_LAYERS)
					return;
				result[0] = layer_count;
				while(layer_count > 0)
				{
					result[layer_count] = makeShort(block[index + 1], block[index]);
					layer_count--;
					index += 2;
				}
				return;
			default:
				log.error("GeoEngine: Unknown block type");
		}
	}

	public static Layer[] NGetLayers(int geoX, int geoY, int refIndex)
	{
		byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, refIndex);

		if(block == null)
			return new Layer[0];

		int cellX, cellY;
		int index = 0;
		// Read current block type: 0 - flat, 1 - complex, 2 - multilevel
		byte type = block[index];
		index++;

		switch(type)
		{
			case BLOCKTYPE_FLAT:
				short height = makeShort(block[index + 1], block[index]);
				height = (short) (height & 0x0fff0);
				return new Layer[]{new Layer(height, NSWE_ALL)};
			case BLOCKTYPE_COMPLEX:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				index += (cellX << 3) + cellY << 1;
				height = makeShort(block[index + 1], block[index]);
				return new Layer[]{new Layer((short) ((short) (height & 0x0fff0) >> 1), (byte) (height & 0x0F))};
			case BLOCKTYPE_MULTILEVEL:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = block[index];
					index += (lc << 1) + 1;
					offset--;
				}
				byte layer_count = block[index];
				index++;
				if(layer_count <= 0 || layer_count > MAX_LAYERS)
					return new Layer[0];
				Layer[] layers = new Layer[layer_count];
				while(layer_count > 0)
				{
					height = makeShort(block[index + 1], block[index]);
					layer_count--;
					layers[layer_count] = new Layer((short) ((short) (height & 0x0fff0) >> 1), (byte) (height & 0x0F));
					index += 2;
				}
				return layers;
			default:
				log.error("GeoEngine: Unknown block type");
				return new Layer[0];
		}
	}

	private static short NgetType(int geoX, int geoY, int refIndex)
	{
		byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, refIndex);

		if(block == null)
			return 0;

		return block[0];
	}

	public static int NgetHeight(int geoX, int geoY, int z, int refIndex)
	{
		byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, refIndex);

		if(block == null)
			return z;

		int cellX, cellY, index = 0;

		// Read current block type: 0 - flat, 1 - complex, 2 - multilevel
		byte type = block[index];
		index++;

		short height;
		switch(type)
		{
			case BLOCKTYPE_FLAT:
				height = makeShort(block[index + 1], block[index]);
				return (short) (height & 0x0fff0);
			case BLOCKTYPE_COMPLEX:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				index += (cellX << 3) + cellY << 1;
				height = makeShort(block[index + 1], block[index]);
				return (short) ((short) (height & 0x0fff0) >> 1); // height / 2
			case BLOCKTYPE_MULTILEVEL:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = block[index];
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = block[index];
				index++;
				if(layers <= 0 || layers > MAX_LAYERS)
					return (short) z;

				int z_nearest_lower_limit = z + Config.MIN_LAYER_HEIGHT;
				int z_nearest_lower = Integer.MIN_VALUE;
				int z_nearest = Integer.MIN_VALUE;

				while(layers > 0)
				{
					height = (short) ((short) (makeShort(block[index + 1], block[index]) & 0x0fff0) >> 1);
					if(height < z_nearest_lower_limit)
						z_nearest_lower = Math.max(z_nearest_lower, height);
					else if(Math.abs(z - height) < Math.abs(z - z_nearest))
						z_nearest = height;
					layers--;
					index += 2;
				}

				return z_nearest_lower != Integer.MIN_VALUE ? z_nearest_lower : z_nearest;
			default:
				log.error("GeoEngine: Unknown blockType");
				return z;
		}
	}

	/**
	 * @param geoX позиция геодаты
	 * @param geoY позиция геодаты
	 * @param z	координата без изменений
	 * @return NSWE: 0-15
	 */
	public static byte NgetNSWE(int geoX, int geoY, int z, int refIndex)
	{
		byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, refIndex);

		if(block == null)
			return NSWE_ALL;

		int cellX, cellY;
		int index = 0;

		// Read current block type: 0 - flat, 1 - complex, 2 - multilevel
		byte type = block[index];
		index++;

		switch(type)
		{
			case BLOCKTYPE_FLAT:
				return NSWE_ALL;
			case BLOCKTYPE_COMPLEX:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				index += (cellX << 3) + cellY << 1;
				short height = makeShort(block[index + 1], block[index]);
				return (byte) (height & 0x0F);
			case BLOCKTYPE_MULTILEVEL:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = block[index];
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = block[index];
				index++;
				if(layers <= 0 || layers > MAX_LAYERS)
					return NSWE_ALL;

				short tempz1 = Short.MIN_VALUE;
				short tempz2 = Short.MIN_VALUE;
				int index_nswe1 = NSWE_NONE;
				int index_nswe2 = NSWE_NONE;
				int z_nearest_lower_limit = z + Config.MIN_LAYER_HEIGHT;

				while(layers > 0)
				{
					height = (short) ((short) (makeShort(block[index + 1], block[index]) & 0x0fff0) >> 1); // height / 2

					if(height < z_nearest_lower_limit)
					{
						if(height > tempz1)
						{
							tempz1 = height;
							index_nswe1 = index;
						}
					}
					else if(Math.abs(z - height) < Math.abs(z - tempz2))
					{
						tempz2 = height;
						index_nswe2 = index;
					}

					layers--;
					index += 2;
				}

				if(index_nswe1 > 0)
					return (byte) (makeShort(block[index_nswe1 + 1], block[index_nswe1]) & 0x0F);
				if(index_nswe2 > 0)
					return (byte) (makeShort(block[index_nswe2 + 1], block[index_nswe2]) & 0x0F);

				return NSWE_ALL;
			default:
				log.error("GeoEngine: Unknown block type.");
				return NSWE_ALL;
		}
	}

	public static void NgetHeightAndNSWE(int geoX, int geoY, short z, short[] result, int refIndex)
	{
		byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, refIndex);

		if(block == null)
		{
			result[0] = z;
			result[1] = NSWE_ALL;
			return;
		}

		int cellX, cellY, index = 0;
		short height, NSWE = NSWE_ALL;

		// Read current block type: 0 - flat, 1 - complex, 2 - multilevel
		byte type = block[index];
		index++;

		switch(type)
		{
			case BLOCKTYPE_FLAT:
				height = makeShort(block[index + 1], block[index]);
				result[0] = (short) (height & 0x0fff0);
				result[1] = NSWE_ALL;
				return;
			case BLOCKTYPE_COMPLEX:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				index += (cellX << 3) + cellY << 1;
				height = makeShort(block[index + 1], block[index]);
				result[0] = (short) ((short) (height & 0x0fff0) >> 1); // height / 2
				result[1] = (short) (height & 0x0F);
				return;
			case BLOCKTYPE_MULTILEVEL:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = block[index];
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = block[index];
				index++;
				if(layers <= 0 || layers > MAX_LAYERS)
				{
					result[0] = z;
					result[1] = NSWE_ALL;
					return;
				}

				short tempz1 = Short.MIN_VALUE;
				short tempz2 = Short.MIN_VALUE;
				int index_nswe1 = 0;
				int index_nswe2 = 0;
				int z_nearest_lower_limit = z + Config.MIN_LAYER_HEIGHT;

				while(layers > 0)
				{
					height = (short) ((short) (makeShort(block[index + 1], block[index]) & 0x0fff0) >> 1); // height / 2

					if(height < z_nearest_lower_limit)
					{
						if(height > tempz1)
						{
							tempz1 = height;
							index_nswe1 = index;
						}
					}
					else if(Math.abs(z - height) < Math.abs(z - tempz2))
					{
						tempz2 = height;
						index_nswe2 = index;
					}

					layers--;
					index += 2;
				}

				if(index_nswe1 > 0)
				{
					NSWE = makeShort(block[index_nswe1 + 1], block[index_nswe1]);
					NSWE = (short) (NSWE & 0x0F);
				}
				else if(index_nswe2 > 0)
				{
					NSWE = makeShort(block[index_nswe2 + 1], block[index_nswe2]);
					NSWE = (short) (NSWE & 0x0F);
				}
				result[0] = tempz1 > Short.MIN_VALUE ? tempz1 : tempz2;
				result[1] = NSWE;
				return;
			default:
				log.error("GeoEngine: Unknown block type.");
				result[0] = z;
				result[1] = NSWE_ALL;
		}
	}

	private static short makeShort(byte b1, byte b0)
	{
		return (short) (b1 << 8 | b0 & 0xff);
	}

	/**
	 * @param geoPos позиция геодаты
	 * @return Block Index: 0-255
	 */
	private static int getBlock(int geoPos)
	{
		return (geoPos >> 3) % 256;
	}

	/**
	 * @param geoPos позиция геодаты
	 * @return Cell Index: 0-7
	 */
	private static int getCell(int geoPos)
	{
		return geoPos % 8;
	}

	/**
	 * Создает индекс блока геодаты по заданым координатам блока.
	 *
	 * @param blockX блок по geoX
	 * @param blockY блок по geoY
	 * @return индекс блока
	 */
	private static int getBlockIndex(int blockX, int blockY)
	{
		return (blockX << 8) + blockY;
	}

	private static byte sign(int x)
	{
		if(x >= 0)
			return +1;
		return -1;
	}

	/**
	 * Возвращает актуальный блок для текущих геокоординат.<BR>
	 * Является заготовкой для возвращения отдельніх блоков с дверьми
	 *
	 * @param geoX геокоордината
	 * @param geoY геокоордината
	 * @return текущий блок геодаты, или null если нет геодаты.
	 */
	private static byte[] getGeoBlockFromGeoCoords(int geoX, int geoY, int refIndex)
	{
		int ix = geoX >> 11;
		int iy = geoY >> 11;

		if(ix < 0 || ix >= L2World.WORLD_SIZE_X || iy < 0 || iy >= L2World.WORLD_SIZE_Y)
			return null;

		byte[][][] region = geodata[ix][iy];

		if(region == null)
			return null;

		int blockX = getBlock(geoX);
		int blockY = getBlock(geoY);
		int index = getBlockIndex(blockX, blockY);

		if(refIndex < 0 || region[index].length <= refIndex || region[index][refIndex] == null)
			refIndex = 0;

		return region[index][refIndex];
	}

	/**
	 * Загрузка геодаты в память
	 */
	public static void loadGeo()
	{
		log.info("GeoEngine: - Loading Geodata...");

		File f = new File("./geodata");

		if(!f.exists() || !f.isDirectory())
		{
			log.info("GeoEngine: Files missing, loading aborted.");
			return;
		}

		int counter = 0;
		Pattern p = Pattern.compile(Config.GEOFILES_PATTERN);
		for(File q : f.listFiles())
		{
			if(q.isDirectory())
				continue;

			String fn = q.getName();
			Matcher m = p.matcher(fn);
			if(m.find())
			{
				switch(Config.GEOFILES_FORMAT)
				{
					case 1:
						loadOFFGeodataFile(Byte.parseByte(m.group(1)), Byte.parseByte(m.group(2)));
						break;
					case 2:
						loadL2JGeodataFile(Byte.parseByte(m.group(1)), Byte.parseByte(m.group(2)));
						break;
					default:
						log.warn("GeoEngine: unknown geodata format: " + Config.GEOFILES_FORMAT);
				}
				counter++;
			}
		}
		log.info("GeoEngine: Loaded " + counter + " map(s), max layers: " + MAX_LAYERS);
	}

/*
	public static void DumpGeodata(String dir)
	{
		new File(dir).mkdirs();
		for(int mapX = 0; mapX < L2World.WORLD_SIZE_X; mapX++)
			for(int mapY = 0; mapY < L2World.WORLD_SIZE_Y; mapY++)
			{
				if(geodata[mapX][mapY] == null)
					continue;
				int rx = mapX + Config.GEO_X_FIRST;
				int ry = mapY + Config.GEO_Y_FIRST;
				String fName = dir + "/" + rx + "_" + ry + ".l2j";
				log.info("Dumping geo: " + fName);
				DumpGeodataFile(fName, (byte) rx, (byte) ry);
			}
	}

	public static boolean DumpGeodataFile(int cx, int cy)
	{
		return DumpGeodataFileMap((byte) (Math.floor((float) cx / (float) 32768) + 20), (byte) (Math.floor((float) cy / (float) 32768) + 18));
	}

	public static boolean DumpGeodataFileMap(byte rx, byte ry)
	{
		String name = "./log/" + rx + "_" + ry + ".l2j";
		return DumpGeodataFile(name, rx, ry);
	}

	public static boolean DumpGeodataFile(String _name, byte rx, byte ry)
	{
		int ix = rx - Config.GEO_X_FIRST;
		int iy = ry - Config.GEO_Y_FIRST;

		byte[][][] geoblocks = geodata[ix][iy];
		if(geoblocks == null)
			return false;

		try
		{
			File f = new File(_name);
			if(f.exists())
				f.delete();
			FileChannel wChannel = new RandomAccessFile(f, "rw").getChannel();

			for(byte[][] geoblock : geoblocks)
			{
				ByteBuffer buffer = ByteBuffer.wrap(geoblock[0]);
				wChannel.write(buffer);
			}
			wChannel.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}

		return true;
	}
*/

	/**
	 * Загрузка региона геодаты.
	 *
	 * @param rx регион x
	 * @param ry регион y
	 */
	public static boolean loadOFFGeodataFile(byte rx, byte ry)
	{
		String fname = "./geodata/" + rx + "_" + ry + "_conv.dat";
		int ix = rx - Config.GEO_X_FIRST;
		int iy = ry - Config.GEO_Y_FIRST;

		if(ix < 0 || iy < 0 || ix > (L2World.MAP_MAX_X >> 15) + Math.abs(L2World.MAP_MIN_X >> 15) || iy > (L2World.MAP_MAX_Y >> 15) + Math.abs(L2World.MAP_MIN_Y >> 15))
		{
			log.info("GeoEngine: File " + fname + " was not loaded!!! ");
			return false;
		}

		File Geo = new File(fname);
		int size, index = 18, block = 0;
		try
		{
			byte[] geo;
			synchronized(geodata)
			{
				// Create a read-only memory-mapped file
				FileChannel roChannel = new RandomAccessFile(Geo, "r").getChannel();
				size = (int) roChannel.size();
				ByteBuffer buffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
				roChannel.close();
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				geo = new byte[buffer.remaining()];
				buffer.get(geo, 0, geo.length);
			}

			if(size >= BlocksInMap * 3 * 2 + 18)
			{
				byte[][][] blocks = new byte[BlocksInMap][1][]; // 256 * 256 блоков в регионе геодаты

				// Indexing geo files, so we will know where each block starts
				for(block = 0; block < blocks.length; block++)
				{
					short type = makeShort(geo[index + 1], geo[index]);
					index += 2;

					byte[] geoBlock;
					switch(type)
					{
						case 0x00: // FLAT
							// Создаем блок геодаты
							geoBlock = new byte[2 + 1];

							// Читаем нужные даные с геодаты
							geoBlock[0] = BLOCKTYPE_FLAT;
							geoBlock[1] = geo[index + 2];
							geoBlock[2] = geo[index + 3];

							// Увеличиваем индекс
							index += 4;

							// Добавляем блок геодаты
							blocks[block][0] = geoBlock;
							break;

						case 0x40:

							// Создаем блок геодаты
							geoBlock = new byte[128 + 1];

							// Читаем даные с геодаты
							geoBlock[0] = BLOCKTYPE_COMPLEX;
							System.arraycopy(geo, index, geoBlock, 1, 128);

							// Увеличиваем индекс
							index += 128;

							// Добавляем блок геодаты
							blocks[block][0] = geoBlock;
							break;

						default:
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							baos.write(BLOCKTYPE_MULTILEVEL);

							// Т.к. у нас нет фиксированой длинны геодаты, то делаем конвертацию на лету
							for(int b = 0; b < 64; b++)
							{
								byte layers = (byte) makeShort(geo[index + 1], geo[index]);
								MAX_LAYERS = Math.max(MAX_LAYERS, layers);

								index += 2;

								baos.write(layers);
								for(int i = 0; i < layers << 1; i++)
									baos.write(geo[index++]);
							}

							// Добавляем даные в масив
							blocks[block][0] = baos.toByteArray();
							//blocks[block][0] = geoBlock;
							break;
					}
				}

				synchronized(geodata)
				{
					geodata[ix][iy] = blocks;
				}
				return true;
			}
			return false;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.warn("Failed to Load GeoFile at block: " + block + "\n");
			return false;
		}
	}

	/**
	 * Загрузка региона геодаты.
	 *
	 * @param rx регион x
	 * @param ry регион y
	 */
	public static boolean loadL2JGeodataFile(byte rx, byte ry)
	{
		String fname = "./geodata/" + rx + "_" + ry + ".l2j";
		int ix = rx - Config.GEO_X_FIRST;
		int iy = ry - Config.GEO_Y_FIRST;

		if(ix < 0 || iy < 0 || ix > (L2World.MAP_MAX_X >> 15) + Math.abs(L2World.MAP_MIN_X >> 15) || iy > (L2World.MAP_MAX_Y >> 15) + Math.abs(L2World.MAP_MIN_Y >> 15))
		{
			log.info("GeoEngine: File " + fname + " was not loaded!!! ");
			return false;
		}

		File Geo = new File(fname);
		int size, index = 0, block = 0;
		try
		{
			byte[] geo;
			synchronized(geodata)
			{
				// Create a read-only memory-mapped file
				FileChannel roChannel = new RandomAccessFile(Geo, "r").getChannel();
				size = (int) roChannel.size();
				ByteBuffer buffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
				roChannel.close();
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				geo = new byte[buffer.remaining()];
				buffer.get(geo, 0, geo.length);
			}
			if(size >= BlocksInMap * 3)
			{
				byte[][][] blocks = new byte[BlocksInMap][1][]; // 256 * 256 блоков в регионе геодаты

				// Indexing geo files, so we will know where each block starts
				for(block = 0; block < blocks.length; block++)
				{
					byte type = geo[index];
					index++;

					byte[] geoBlock;
					switch(type)
					{
						case BLOCKTYPE_FLAT:

							// Создаем блок геодаты
							geoBlock = new byte[2 + 1];

							// Читаем нужные даные с геодаты
							geoBlock[0] = type;
							geoBlock[1] = geo[index];
							geoBlock[2] = geo[index + 1];

							// Увеличиваем индекс
							index += 2;

							// Добавляем блок геодаты
							blocks[block][0] = geoBlock;
							break;

						case BLOCKTYPE_COMPLEX:

							// Создаем блок геодаты
							geoBlock = new byte[128 + 1];

							// Читаем даные с геодаты
							geoBlock[0] = type;
							System.arraycopy(geo, index, geoBlock, 1, 128);

							// Увеличиваем индекс
							index += 128;

							// Добавляем блок геодаты
							blocks[block][0] = geoBlock;
							break;

						case BLOCKTYPE_MULTILEVEL:
							// Оригинальный индекс
							int orgIndex = index;

							// Считаем длину блока геодаты
							for(int b = 0; b < 64; b++)
							{
								byte layers = geo[index];
								MAX_LAYERS = Math.max(MAX_LAYERS, layers);
								index += (layers << 1) + 1;
							}

							// Получаем длину
							int diff = index - orgIndex;

							// Создаем массив геодаты
							geoBlock = new byte[diff + 1];

							// Читаем даные с геодаты
							geoBlock[0] = type;
							System.arraycopy(geo, orgIndex, geoBlock, 1, diff);

							// Добавляем блок геодаты
							blocks[block][0] = geoBlock;
							break;
						default:
							log.error("GeoEngine: invalid block type: " + type);
					}
				}

				synchronized(geodata)
				{
					geodata[ix][iy] = blocks;
				}
				return true;
			}
			return false;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.warn("Failed to Load GeoFile at block: " + block + "\n");
			return false;
		}
	}

	public static void reloadGeo(int x, int y)
	{
		switch(Config.GEOFILES_FORMAT)
		{
			case 1:
				loadOFFGeodataFile((byte) x, (byte) y);
				break;
			case 2:
				loadL2JGeodataFile((byte) x, (byte) y);
				break;
			default:
				log.warn("GeoEngine: unknown geodata format: " + Config.GEOFILES_FORMAT);
		}
	}

	/**
	 * Преобразовывает FLAT блоки в COMPLEX<br>
	 *
	 * @param ix		 регион x
	 * @param iy		 регион y
	 * @param blockIndex индекс блока в регионе
	 */
	private static void copyBlock(int ix, int iy, int blockIndex)
	{
		byte[][][] region = geodata[ix][iy];

		if(region == null)
		{
			log.info("door at null region? [" + ix + "][" + iy + "]");
			return;
		}

		byte[] block = region[blockIndex][0];
		byte blockType = block[0];

		switch(blockType)
		{
			case BLOCKTYPE_FLAT:
				short height = makeShort(block[2], block[1]);
				height &= 0x0fff0;
				height <<= 1;
				height |= NORTH;
				height |= SOUTH;
				height |= WEST;
				height |= EAST;
				byte[] newblock = new byte[129];
				newblock[0] = BLOCKTYPE_COMPLEX;
				for(int i = 1; i < 129; i += 2)
				{
					newblock[i + 1] = (byte) (height >> 8);
					newblock[i] = (byte) (height & 0x00ff);
				}
				region[blockIndex][0] = newblock;
				break;
		}
	}

	private static boolean check_door_z(int minZ, int maxZ, int geoZ)
	{
		return minZ <= geoZ && geoZ <= maxZ || Math.abs((minZ + maxZ) / 2 - geoZ) <= Door_MaxZDiff;
	}

	private static boolean check_cell_in_door(int geoX, int geoY, L2Territory pos)
	{
		geoX = (geoX << 4) + L2World.MAP_MIN_X + 8;
		geoY = (geoY << 4) + L2World.MAP_MIN_Y + 8;
		for(int ax = geoX; ax < geoX + 16; ax++)
			for(int ay = geoY; ay < geoY + 16; ay++)
				if(pos.isInside(ax, ay))
					return true;
		return false;
	}

	public static void returnGeoAtControl(GeoControl control)
	{
		L2Territory pos = control.getGeoPos();
		HashMap<Long, Byte> around = control.getGeoAround();
		int refIndex = control.getReflection();

		if(around == null)
		{
			log.info("GeoEngine: Attempt to open 'not closed' door");
			Thread.dumpStack();
			return;
		}

		short height;
		byte old_nswe;

		if(control.getReflection() > 0 && Config.DEBUG_INSTANCES)
			Instance._log.info(control + " open geo.");

		synchronized(geodata)
		{
			for(long geoXY : around.keySet())
			{
				int geoX = (int) geoXY;
				int geoY = (int) (geoXY >> 32);

				// Получение мировых координат
				int ix = geoX >> 11;
				int iy = geoY >> 11;

				// Получение индекса блока
				int blockX = getBlock(geoX);
				int blockY = getBlock(geoY);
				int blockIndex = getBlockIndex(blockX, blockY);

				byte[][][] region = geodata[ix][iy];
				if(region == null)
				{
					log.info("GeoEngine: Attempt to open door at block with no geodata");
					return;
				}

				if(region[blockIndex].length <= refIndex || region[blockIndex][refIndex] == null)
				{
					log.info("GeoEngine: Attempt to open door at not existent reflection: " + refIndex);
					return;
				}

				byte[] block = region[blockIndex][refIndex];

				int cellX = getCell(geoX);
				int cellY = getCell(geoY);

				int index = 0;
				byte blockType = block[index];
				index++;

				switch(blockType)
				{
					case BLOCKTYPE_COMPLEX:
						index += (cellX << 3) + cellY << 1;

						// Получаем высоту клетки
						height = makeShort(block[index + 1], block[index]);
						old_nswe = (byte) (height & 0x0F);
						height &= 0xfff0;
						height >>= 1;

						if(control.getReflection() > 0 && Config.DEBUG_INSTANCES)
							Instance._log.info(control + " open geo: h=" + height + " cx=" + cellX + " cy=" + cellY + " " + getNSWE(old_nswe) + " " + getNSWE((byte)(old_nswe | around.get(geoXY))));
						// around
						height <<= 1;
						height &= 0xfff0;
						height |= old_nswe;
						height |= around.get(geoXY);

						// Записываем высоту в массив
						block[index + 1] = (byte) (height >> 8);
						block[index] = (byte) (height & 0x00ff);
						break;
					case BLOCKTYPE_MULTILEVEL:
						// Последний валидный индекс для двери
						int neededIndex = -1;

						// Далее следует стандартный механизм получения высоты
						int offset = (cellX << 3) + cellY;
						while(offset > 0)
						{
							byte lc = block[index];
							index += (lc << 1) + 1;
							offset--;
						}
						byte layers = block[index];
						index++;
						if(layers <= 0 || layers > MAX_LAYERS)
							break;
						short temph = Short.MIN_VALUE;
						old_nswe = NSWE_ALL;
						while(layers > 0)
						{
							height = makeShort(block[index + 1], block[index]);
							byte tmp_nswe = (byte) (height & 0x0F);
							height &= 0xfff0;
							height >>= 1;
							int z_diff_last = Math.abs(pos.getZmin() - temph);
							int z_diff_curr = Math.abs(pos.getZmin() - height);
							if(z_diff_last > z_diff_curr)
							{
								old_nswe = tmp_nswe;
								temph = height;
								neededIndex = index;
							}
							layers--;
							index += 2;
						}

						if(control.getReflection() > 0 && Config.DEBUG_INSTANCES)
							Instance._log.info(control + " open geo: h=" + temph + " cx=" + cellX + " cy=" + cellY + " " + getNSWE(old_nswe) + " " + getNSWE((byte)(old_nswe | around.get(geoXY))));
						// around
						temph <<= 1;
						temph &= 0xfff0;
						temph |= old_nswe;
						temph |= around.get(geoXY);

						// записываем высоту
						block[neededIndex + 1] = (byte) (temph >> 8);
						block[neededIndex] = (byte) (temph & 0x00ff);
						break;
				}
			}
		}
	}

	public static void applyControl(GeoControl control)
	{
		L2Territory pos = control.getGeoPos();

		if(pos == null)
		{
			log.warn("GeoEngine: no _geoPos for door: " + control);
			return;
		}

		HashMap<Long, Byte> around = control.getGeoAround();
		int refIndex = control.getReflection();

		boolean first_time = around == null;

		if(control.getReflection() > 0 && Config.DEBUG_INSTANCES)
			Instance._log.info(control + " close geo.");

		if(around == null)
		{
			around = new HashMap<Long, Byte>();
			GArray<Long> around_blocks = new GArray<Long>();
			int minX = pos.getXmin() - L2World.MAP_MIN_X >> 4;
			int maxX = pos.getXmax() - L2World.MAP_MIN_X >> 4;
			int minY = pos.getYmin() - L2World.MAP_MIN_Y >> 4;
			int maxY = pos.getYmax() - L2World.MAP_MIN_Y >> 4;
			for(int geoX = minX; geoX <= maxX; geoX++)
				for(int geoY = minY; geoY <= maxY; geoY++)
					if(check_cell_in_door(geoX, geoY, pos))
						around_blocks.add(makeLong(geoX, geoY));

			for(long geoXY : around_blocks)
			{
				int geoX = (int) geoXY;
				int geoY = (int) (geoXY >> 32);
				long aroundN_geoXY = makeLong(geoX, geoY - 1); // close S
				long aroundS_geoXY = makeLong(geoX, geoY + 1); // close N
				long aroundW_geoXY = makeLong(geoX - 1, geoY); // close E
				long aroundE_geoXY = makeLong(geoX + 1, geoY); // close W
				around.put(geoXY, NSWE_ALL);
				byte _nswe;
				if(!around_blocks.contains(aroundN_geoXY))
				{
					_nswe = around.containsKey(aroundN_geoXY) ? around.remove(aroundN_geoXY) : 0;
					_nswe |= SOUTH;
					around.put(aroundN_geoXY, _nswe);
				}
				if(!around_blocks.contains(aroundS_geoXY))
				{
					_nswe = around.containsKey(aroundS_geoXY) ? around.remove(aroundS_geoXY) : 0;
					_nswe |= NORTH;
					around.put(aroundS_geoXY, _nswe);
				}
				if(!around_blocks.contains(aroundW_geoXY))
				{
					_nswe = around.containsKey(aroundW_geoXY) ? around.remove(aroundW_geoXY) : 0;
					_nswe |= EAST;
					around.put(aroundW_geoXY, _nswe);
				}
				if(!around_blocks.contains(aroundE_geoXY))
				{
					_nswe = around.containsKey(aroundE_geoXY) ? around.remove(aroundE_geoXY) : 0;
					_nswe |= WEST;
					around.put(aroundE_geoXY, _nswe);
				}
			}
			around_blocks.clear();
			control.setGeoAround(around);
		}

		short height;
		byte old_nswe, close_nswe;

		synchronized(geodata)
		{
			Long[] around_keys = around.keySet().toArray(new Long[around.size()]);
			for(long geoXY : around_keys)
			{
				int geoX = (int) geoXY;
				int geoY = (int) (geoXY >> 32);

				// Получение мировых координат
				int ix = geoX >> 11;
				int iy = geoY >> 11;

				// Получение индекса блока
				int blockX = getBlock(geoX);
				int blockY = getBlock(geoY);
				int blockIndex = getBlockIndex(blockX, blockY);

				// Попытка скопировать блок геодаты, если уже существует, то не скопируется
				if(first_time)
					copyBlock(ix, iy, blockIndex);

				byte[][][] region = geodata[ix][iy];
				if(region == null)
				{
					log.info("GeoEngine: Attempt to close door at block with no geodata");
					return;
				}

				if(region[blockIndex].length <= refIndex)
				{
					if(control.getReflection() > 0 && Config.DEBUG_INSTANCES)
						Instance._log.info(control + " create geo: " + ix + " " + iy + " " + blockIndex);

					byte[][] refBlock = new byte[refIndex + 1][region[blockIndex][0].length];
					System.arraycopy(region[blockIndex], 0, refBlock, 0, region[blockIndex].length);
					for(int i = region[blockIndex].length; i < refIndex; i++)
						refBlock[i] = null;
					System.arraycopy(region[blockIndex][0], 0, refBlock[refIndex], 0, region[blockIndex][0].length);
					region[blockIndex] = refBlock;
				}
				else if(region[blockIndex][refIndex] == null || region[blockIndex][refIndex][0] == 0)
				{
					if(control.getReflection() > 0 && Config.DEBUG_INSTANCES)
					{
						Instance._log.info(control + " create geo1: " + ix + " " + iy + " " + blockIndex);
						if(region[blockIndex][refIndex] != null && region[blockIndex][refIndex][0] == 0)
							Instance._log.info(control + " create geo1: empty block WTF??!!");
					}
					byte[] refBlock = new byte[region[blockIndex][0].length];
					System.arraycopy(region[blockIndex][0], 0, refBlock, 0, region[blockIndex][0].length);
					region[blockIndex][refIndex] = refBlock;
				}

				byte[] block = region[blockIndex][refIndex];

				int cellX = getCell(geoX);
				int cellY = getCell(geoY);

				int index = 0;
				byte blockType = block[index];
				index++;

				switch(blockType)
				{
					case BLOCKTYPE_COMPLEX:
						index += (cellX << 3) + cellY << 1;

						// Получаем высоту клетки
						height = makeShort(block[index + 1], block[index]);
						old_nswe = (byte) (height & 0x0F);
						height &= 0xfff0;
						height >>= 1;

						if(first_time)
						{
							close_nswe = around.remove(geoXY);
							// подходящий слой не найден
							if(!check_door_z(pos.getZmin(), pos.getZmax(), height))
								break;
							close_nswe &= old_nswe;
							around.put(geoXY, close_nswe);
						}
						else
							close_nswe = around.get(geoXY);

						if(control.getReflection() > 0 && Config.DEBUG_INSTANCES)
							Instance._log.info(control + " close geo: h=" + height + " cx=" + cellX + " cy=" + cellY + " " + getNSWE(old_nswe) + " " + getNSWE((byte)(old_nswe & ~close_nswe)));
						// around
						height <<= 1;
						height &= 0xfff0;
						height |= old_nswe;
						height &= ~close_nswe;

						// Записываем высоту в массив
						block[index + 1] = (byte) (height >> 8);
						block[index] = (byte) (height & 0x00ff);
						break;
					case BLOCKTYPE_MULTILEVEL:
						// Последний валидный индекс для двери
						int neededIndex = -1;

						// Далее следует стандартный механизм получения высоты
						int offset = (cellX << 3) + cellY;
						while(offset > 0)
						{
							byte lc = block[index];
							index += (lc << 1) + 1;
							offset--;
						}
						byte layers = block[index];
						index++;
						if(layers <= 0 || layers > MAX_LAYERS)
							break;
						short temph = Short.MIN_VALUE;
						old_nswe = NSWE_ALL;
						while(layers > 0)
						{
							height = makeShort(block[index + 1], block[index]);
							byte tmp_nswe = (byte) (height & 0x0F);
							height &= 0xfff0;
							height >>= 1;
							int z_diff_last = Math.abs(pos.getZmin() - temph);
							int z_diff_curr = Math.abs(pos.getZmin() - height);
							if(z_diff_last > z_diff_curr)
							{
								old_nswe = tmp_nswe;
								temph = height;
								neededIndex = index;
							}
							layers--;
							index += 2;
						}

						if(first_time)
						{
							close_nswe = around.remove(geoXY);
							// подходящий слой не найден
							if(temph == Short.MIN_VALUE || !check_door_z(pos.getZmin(), pos.getZmax(), temph))
								break;

							close_nswe &= old_nswe;
							around.put(geoXY, close_nswe);
						}
						else
							close_nswe = around.get(geoXY);

						if(control.getReflection() > 0 && Config.DEBUG_INSTANCES)
							Instance._log.info(control + " close geo: h=" + temph + " cx=" + cellX + " cy=" + cellY + " " + getNSWE(old_nswe) + " " + getNSWE((byte)(old_nswe & ~close_nswe)));
						// around
						temph <<= 1;
						temph &= 0xfff0;
						temph |= old_nswe;
						temph &= ~close_nswe;

						// записываем высоту
						block[neededIndex + 1] = (byte) (temph >> 8);
						block[neededIndex] = (byte) (temph & 0x00ff);
						break;
				}
			}
		}
	}

	public static void deleteControl(GeoControl control)
	{
		int refIndex = control.getReflection();

		if(refIndex > 0)
		{
			if(Config.DEBUG_INSTANCES)
				Instance._log.info(control + " delete geo.");

			HashMap<Long, Byte> around = control.getGeoAround();
			if(around != null)
			{
				synchronized(geodata)
				{
					for(long geoXY : around.keySet())
					{
						int geoX = (int) geoXY;
						int geoY = (int) (geoXY >> 32);

						// Получение мировых координат
						int ix = geoX >> 11;
						int iy = geoY >> 11;

						// Получение индекса блока
						int blockX = getBlock(geoX);
						int blockY = getBlock(geoY);
						int blockIndex = getBlockIndex(blockX, blockY);

						byte[][][] region = geodata[ix][iy];
						if(region == null)
						{
							log.info("GeoEngine: Attempt to close door at block with no geodata");
							return;
						}

						if(region[blockIndex].length > refIndex && region[blockIndex][refIndex] != null)
						{
							if(Config.DEBUG_INSTANCES)
								Instance._log.info(control + " delete geo: " + ix + " " + iy + " " + blockIndex);
							region[blockIndex][refIndex] = null;
						}
					}
				}
			}
		}
	}

	private static long makeLong(int nLo, int nHi)
	{
		return (long) nHi << 32 | nLo & 0x00000000ffffffffL;
	}

	public static Location findPointToStay(int x, int y, int z, int j, int k, int refIndex)
	{
		Location pos;
		for(int i = 0; i < 100; i++)
		{
			pos = Location.coordsRandomize(x, y, z, 0, j, k);
			if(canMoveToCoord(x, y, z, pos.getX(), pos.getY(), pos.getZ(), refIndex) && canMoveToCoord(pos.getX(), pos.getY(), pos.getZ(), x, y, z, refIndex))
				return pos;
		}
		return new Location(x, y, z);
	}

	public static void unload()
	{
		for(int mapX = 0; mapX < L2World.WORLD_SIZE_X; mapX++)
			for(int mapY = 0; mapY < L2World.WORLD_SIZE_Y; mapY++)
				geodata[mapX][mapY] = null;
	}

	private static String getNSWE(byte nswe)
	{
		return ((nswe & NORTH) == NORTH ? "|N" : "| ") + ((nswe & SOUTH) == SOUTH ? "S" : " ") + ((nswe & WEST) == WEST ? "W" : " ") + ((nswe & EAST) == EAST ? "E|" : " |");
	}
}
