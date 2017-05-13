package ru.l2gw.gameserver.model;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.ZoneManager;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;

import java.awt.*;
import java.io.Serializable;

@SuppressWarnings("serial")
public class L2Territory implements Serializable
{
	private static Log _log = LogFactory.getLog(L2Territory.class.getName());

	@SuppressWarnings("serial")
	public class Point implements Serializable
	{
		public int x, y, zmin, zmax;

		Point(int _x, int _y, int _zmin, int _zmax)
		{
			x = _x;
			y = _y;
			zmin = _zmin;
			zmax = _zmax;
		}
	}

	private L2Zone _zone;
	private Polygon poly;
	private Point[] _points;
	protected int _x_min;
	protected int _x_max;
	protected int _y_min;
	protected int _y_max;
	protected int _z_min;
	protected int _z_max;

	private String _name;
	private GArray<L2Territory> _banned;

	public L2Territory(String name)
	{
		poly = new Polygon();
		_points = new Point[0];
		_x_min = 999999;
		_x_max = -999999;
		_y_min = 999999;
		_y_max = -999999;
		_z_min = 999999;
		_z_max = -999999;
		_name = name;
	}

	public void add(int x, int y, int zmin, int zmax)
	{
		Point[] newPoints = new Point[_points.length + 1];
		System.arraycopy(_points, 0, newPoints, 0, _points.length);
		newPoints[_points.length] = new Point(x, y, zmin, zmax);
		_points = newPoints;

		poly.addPoint(x, y);

		if(x < _x_min)
			_x_min = x;
		if(y < _y_min)
			_y_min = y;
		if(x > _x_max)
			_x_max = x;
		if(y > _y_max)
			_y_max = y;
		if(zmin < _z_min)
			_z_min = zmin;
		if(zmax > _z_max)
			_z_max = zmax;
	}

	public void print()
	{
		for(Point p : _points)
			System.out.println("(" + p.x + "," + p.y + ")");
	}

	public boolean isInside(int x, int y)
	{
		return poly.contains(x, y);
	}

	public boolean isInside(int x, int y, int z)
	{
		return z >= _z_min && z <= _z_max && poly.contains(x, y);
	}

	public int[] getRandomPoint(boolean air)
	{
		int i;
		int[] p = new int[3];

		for(i = 0; i < 100; i++)
		{
			p[0] = Rnd.get(_x_min, _x_max);
			p[1] = Rnd.get(_y_min, _y_max);

			// Для отлова проблемных территорый, вызывающих сильную нагрузку
			if(i == 40)
				_log.warn("Heavy territory: " + this + ", need manual correction");

			if(poly.contains(p[0], p[1]))
			{
				// Не спаунить в зоны, запрещенные для спауна
				if(ZoneManager.getInstance().isInsideZone(ZoneType.no_spawn, p[0], p[1]) != null)
					continue;

				if(_banned != null)
				{
					boolean banned = false;
					for(L2Territory terr : _banned)
						if(terr.isInside(p[0], p[1]))
						{
							banned = true;
							break;
						}

					if(banned)
						continue;
				}

				// Не спаунить в колонны, стены и прочее.
				if(Config.GEODATA_ENABLED)
				{
					int tempz = air ? Rnd.get(_z_min, _z_max) : GeoEngine.getHeight(p[0], p[1], _z_min + (_z_max - _z_min) / 2, 0);
					if(_z_min != _z_max)
					{
						if(tempz < _z_min || tempz > _z_max || _z_min > _z_max)
							continue;
					}
					else if(tempz < _z_min - 200 || tempz > _z_min + 200)
						continue;

					p[2] = tempz;

					if(GeoEngine.getNSWE(p[0], p[1], p[2], 0) != 15)
						continue;

					return p;
				}

				double curdistance = -1;
				p[2] = _z_min;

				for(i = 0; i < _points.length; i++)
				{
					Point p1 = _points[i];
					long dx = p1.x - p[0];
					long dy = p1.y - p[1];
					double sqdistance = dx * dx + dy * dy;
					if(curdistance == -1 || sqdistance < curdistance)
					{
						curdistance = sqdistance;
						p[2] = p1.zmin;
					}
				}
				return p;
			}
		}
		_log.warn("Can't make point for " + this);
		return p;
	}

	public final String getName()
	{
		return _name;
	}

	@Override
	public final String toString()
	{
		return "L2Territory: " + _name + "[" + getXmin() + "," + getYmin() + "," + getXmax() + "," + getYmax() + "," + getZmin() + "," + getZmax() + "]";
	}

	public int getZmin()
	{
		return _z_min;
	}

	public int getZmax()
	{
		return _z_max;
	}

	public int getXmax()
	{
		return _x_max;
	}

	public int getXmin()
	{
		return _x_min;
	}

	public int getYmax()
	{
		return _y_max;
	}

	public int getYmin()
	{
		return _y_min;
	}

	public void setZone(L2Zone zone)
	{
		_zone = zone;
	}

	public L2Zone getZone()
	{
		return _zone;
	}

	public FastList<int[]> getCoords()
	{
		FastList<int[]> result = new FastList<int[]>();
		for(Point point : _points)
			result.add(new int[] { point.x, point.y, point.zmin, point.zmax });
		return result;
	}

	public Point[] getPoints()
	{
		return _points;
	}

	public void addBannedTerritory(L2Territory terr)
	{
		if(terr == null)
			return;

		if(_banned == null)
			_banned = new GArray<L2Territory>(1);

		_banned.add(terr);
	}
}