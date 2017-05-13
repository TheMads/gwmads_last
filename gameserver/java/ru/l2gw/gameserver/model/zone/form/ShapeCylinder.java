package ru.l2gw.gameserver.model.zone.form;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 19.12.2008
 * Time: 16:19:20
 */
public class ShapeCylinder extends Shape
{
	private int _radius;
	private long _radiusSq;
	private int _x, _y, _xMin, _xMax, _yMin, _yMax;

	@Override
	public boolean contains(int x, int y)
	{
		return (Math.pow(_x - x, 2) + Math.pow(_y - y, 2)) <= _radiusSq;
	}

	@Override
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		// Circles point inside the rectangle?
		if(_x > ax1 && _x < ax2 && _y > ay1 && _y < ay2) return true;

		// Any point of the rectangle intersecting the Circle?
		if((Math.pow(ax1 - _x, 2) + Math.pow(ay1 - _y, 2)) < _radiusSq) return true;
		if((Math.pow(ax1 - _x, 2) + Math.pow(ay2 - _y, 2)) < _radiusSq) return true;
		if((Math.pow(ax2 - _x, 2) + Math.pow(ay1 - _y, 2)) < _radiusSq) return true;
		if((Math.pow(ax2 - _x, 2) + Math.pow(ay2 - _y, 2)) < _radiusSq) return true;

		// Collision on any side of the rectangle?
		if(_x > ax1 && _x < ax2)
		{
			if(Math.abs(_y - ay2) < _radius) return true;
			if(Math.abs(_y - ay1) < _radius) return true;
		}
		if(_y > ay1 && _y < ay2)
		{
			if(Math.abs(_x - ax2) < _radius) return true;
			if(Math.abs(_x - ax1) < _radius) return true;
		}

		return false;
	}

	@Override
	public double getDistanceToZone(int x, int y)
	{
		return (Math.sqrt(Math.pow(_x - x, 2) + Math.pow(_y - y, 2)) - _radius);
	}

	public void setRadius(int rad)
	{
		_radius = rad;
		_radiusSq = rad * rad;
	}

	@Override
	protected Shape prepare()
	{
		if(_points.size() != 1)
		{
			_log.error("Invalid point amount in zone " + _zoneId + ", must be 1");
			return null;
		}
		if(_radius <= 0)
		{
			_log.error("Radius must be > 0 in zone " + _zoneId);
			return null;
		}

		_x = _points.get(0).x;
		_y = _points.get(0).y;
		_xMin = _x - _radius;
		_xMax = _x + _radius;
		_yMin = _y - _radius;
		_yMax = _y + _radius;

		return this;
	}

	public void setXYZ(int x, int y, int zMin, int zMax)
	{
		_x = x;
		_y = y;
		_zMin = zMin;
		_zMax = zMax;
		_checkZ = !(_zMin == _zMax);
	}

	public int getXMin()
	{
		return _xMin;
	}

	public int getXMax()
	{
		return _xMax;
	}

	public int getYMin()
	{
		return _yMin;
	}

	public int getYMax()
	{
		return _yMax;
	}

	public GArray<Location> getPolygon()
	{
		GArray<Location> poly = new GArray<>(11);
		Location center = new Location(_x, _y, _zMin);
		for(int i = 0; i < 360; i += 30)
			poly.add(Util.getPointInRadius(center, _radius, i).setH(_zMax));

		return poly;
	}
}
