package ru.l2gw.util;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2World;

public class Location
{
	private int x;
	private int y;
	private int z;
	/** Heading */
	private int h = 0;

	/**
	 * Позиция (x, y, z)
	 */
	public Location(int locX, int locY, int locZ)
	{
		x = locX;
		y = locY;
		z = locZ;
	}

	/**
	 * Позиция (x, y, z, heading)
	 */
	public Location(int locX, int locY, int locZ, int heading)
	{
		x = locX;
		y = locY;
		z = locZ;
		h = heading;
	}

	private Location(L2Object obj)
	{
		x = obj.getX();
		y = obj.getY();
		z = obj.getZ();
		h = obj.getHeading();
	}

	@Override
	public boolean equals(Object loc)
	{
		return loc instanceof Location && ((Location)loc).getX() == x && ((Location)loc).getY() == y && ((Location)loc).getZ() == z;
	}

	public boolean equals(int _x, int _y, int _z)
	{
		return _x == x && _y == y && _z == z;
	}

	public boolean equals(int _x, int _y, int _z, int _h)
	{
		return _x == x && _y == y && _z == z && h == _h;
	}

	public void set(int _x, int _y, int _z)
	{
		x = _x;
		y = _y;
		z = _z;
	}

	public void set(int _x, int _y, int _z, int _h)
	{
		x = _x;
		y = _y;
		z = _z;
		h = _h;
	}

	public void set(Location loc)
	{
		x = loc.x;
		y = loc.y;
		z = loc.z;
		h = loc.h;
	}
	
	public Location changeZ(int zDiff)
	{
		z += zDiff;
		return this;
	}

	public Location setX(int x)
	{
		this.x = x;
		return this;
	}

	public Location setY(int y)
	{
		this.y = y;
		return this;
	}

	public Location setZ(int z)
	{
		this.z = z;
		return this;
	}

	public Location setH(int h)
	{
		this.h = h;
		return this;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getZ()
	{
		return z;
	}

	public int getHeading()
	{
		return h;
	}

	public static Location getAroundPosition(L2Object obj, L2Object obj2, int radius_min, int radius_max, int max_geo_checks)
	{
		int x;
		int y;
		int z;
		Location pos = new Location(obj);
		if(radius_min < 0)
			radius_min = 0;
		if(radius_max < 0)
			radius_max = 0;

		float col_radius = obj.getColRadius() + obj2.getColRadius();

		int min_angle = 0;
		int max_angle = 360;
		if(!(obj.equals(obj2)))
		{
			double perfect_angle = Util.calculateAngleFrom(obj, obj2);
			min_angle = (int) perfect_angle - 225;
			min_angle = (int) perfect_angle + 135;
		}

		while(true)
		{
			int randomRadius = Rnd.get(radius_min, radius_max);
			int randomAngle = Rnd.get(min_angle, max_angle);
			x = pos.x + (int) ((col_radius + randomRadius) * Math.cos(randomAngle));
			y = pos.y + (int) ((col_radius + randomRadius) * Math.sin(randomAngle));
			z = pos.z;
			if(max_geo_checks <= 0)
				break;
			z = GeoEngine.getHeight(x, y, z, 0);
			if((Math.abs(pos.z - z) < 256) && (GeoEngine.getNSWE(x, y, z, 0) == 15))
				break;
			--max_geo_checks;
		}

		pos.x = x;
		pos.y = y;
		pos.z = z;
		return pos;
	}

	public static Location parseLoc(String loc)
	{
		if(loc != null)
		{
			try
			{
				String[] coord = loc.split("[,; ]");
				if(coord.length > 2)
				{
					int x = Integer.parseInt(coord[0].trim());
					int y = Integer.parseInt(coord[1].trim());
					int z = Integer.parseInt(coord[2].trim());
					int h = 0;
					if(coord.length > 3)
						h = Integer.parseInt(coord[3]);

					return new Location(x, y, z, h);
				}
			}
			catch(Exception e)
			{
			}
		}
		return null;
	}

	public Location world2geo()
	{
		x = x - L2World.MAP_MIN_X >> 4;
		y = y - L2World.MAP_MIN_Y >> 4;
		return this;
	}

	public Location geo2world()
	{
		// размер одного блока 16*16 точек, +8*+8 это его средина
		x = (x << 4) + L2World.MAP_MIN_X + 8;
		y = (y << 4) + L2World.MAP_MIN_Y + 8;
		return this;
	}

	public final double getDistance(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	public double distance(Location loc)
	{
		if(loc == null)
			return 0;
		long dx = x - loc.x;
		long dy = y - loc.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public double distance3D(Location loc)
	{
		if(loc == null)
			return 0;
		long dx = x - loc.x;
		long dy = y - loc.y;
		long dz = z - loc.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public Location clone()
	{
		return new Location(x, y, z, h);
	}

	public static Location coordsRandomize(L2Object obj, int min, int max)
	{
		return coordsRandomize(obj.getLoc(), min, max);
	}

	public static Location coordsRandomize(L2Object obj, int radius)
	{
		return coordsRandomize(obj, 0, radius);
	}

	public static Location coordsRandomize(int x, int y, int z, int heading, int radius_min, int radius_max)
	{
		if(radius_max == 0 || radius_max < radius_min)
			return new Location(x, y, z, heading);
		int radius = Rnd.get(radius_min, radius_max);
		double angle = Rnd.nextDouble() * 2 * Math.PI;
		return new Location((int) (x + radius * Math.cos(angle)), (int) (y + radius * Math.sin(angle)), z, heading);
	}

	public static Location coordsRandomize(Location pos, int radius)
	{
		return coordsRandomize(pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), 0, radius);
	}

	public static Location coordsRandomize(Location pos, int radius_min, int radius_max)
	{
		return coordsRandomize(pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), radius_min, radius_max);
	}

	public Location correctGeoZ()
	{
		z = GeoEngine.getHeight(x, y, z, 0);
		return this;
	}

	public Location correctGeoZ(int refIndex)
	{
		z = GeoEngine.getHeight(x, y, z, refIndex);
		return this;
	}

	@Override
	public final String toString()
	{
		return "Coords(" + x + "," + y + "," + z + "," + h + ")";
	}
}