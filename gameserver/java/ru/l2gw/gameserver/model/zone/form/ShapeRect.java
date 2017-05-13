package ru.l2gw.gameserver.model.zone.form;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

public class ShapeRect extends Shape
{
	private int _xMin, _xMax;
	private int _yMin, _yMax;

	@Override
	public boolean contains(int x, int y)
	{
		return x >= _xMin && x <= _xMax && y >= _yMin && y <= _yMax;
	}

	@Override
	public boolean intersectsRectangle(int axMin, int axMax, int ayMin, int ayMax)
	{
		// Check if any point inside this rectangle
		if(contains(axMin, ayMin)) return true;
		if(contains(axMin, ayMax)) return true;
		if(contains(axMax, ayMin)) return true;
		if(contains(axMax, ayMax)) return true;

		// Check if any point from this rectangle is inside the other one
		if(_xMin > axMin && _xMin < axMax && _yMin > ayMin && _yMin < ayMax) return true;
		if(_xMin > axMin && _xMin < axMax && _yMax > ayMin && _yMax < ayMax) return true;
		if(_xMax > axMin && _xMax < axMax && _yMin > ayMin && _yMin < ayMax) return true;
		if(_xMax > axMin && _xMax < axMax && _yMax > ayMin && _yMax < ayMax) return true;

		// Horizontal lines may intersect vertical lines
		if(lineSegmentsIntersect(_xMin, _yMin, _xMax, _yMin, axMin, ayMin, axMin, ayMax)) return true;
		if(lineSegmentsIntersect(_xMin, _yMin, _xMax, _yMin, axMax, ayMin, axMax, ayMax)) return true;
		if(lineSegmentsIntersect(_xMin, _yMax, _xMax, _yMax, axMin, ayMin, axMin, ayMax)) return true;
		if(lineSegmentsIntersect(_xMin, _yMax, _xMax, _yMax, axMax, ayMin, axMax, ayMax)) return true;

		// Vertical lines may intersect horizontal lines
		if(lineSegmentsIntersect(_xMin, _yMin, _xMin, _yMax, axMin, ayMin, axMax, ayMin)) return true;
		if(lineSegmentsIntersect(_xMin, _yMin, _xMin, _yMax, axMin, ayMax, axMax, ayMax)) return true;
		if(lineSegmentsIntersect(_xMax, _yMin, _xMax, _yMax, axMin, ayMin, axMax, ayMin)) return true;
		if(lineSegmentsIntersect(_xMax, _yMin, _xMax, _yMax, axMin, ayMax, axMax, ayMax)) return true;

		return false;
	}

	@Override
	public double getDistanceToZone(int x, int y)
	{
		double test, shortestDist = Math.pow(_xMin - x, 2) + Math.pow(_yMin - y, 2);

		test = Math.pow(_xMin - x, 2) + Math.pow(_yMax - y, 2);
		if(test < shortestDist) shortestDist = test;

		test = Math.pow(_xMax - x, 2) + Math.pow(_yMin - y, 2);
		if(test < shortestDist) shortestDist = test;

		test = Math.pow(_xMax - x, 2) + Math.pow(_yMax - y, 2);
		if(test < shortestDist) shortestDist = test;

		return Math.sqrt(shortestDist);
	}

	@Override
	protected Shape prepare()
	{
		if(_points.size() != 2)
		{
			_log.error("Invalid point amount in zone " + _zoneId + ", must be 2");
			return null;
		}

		if(_points.get(0).x < _points.get(1).x)
		{
			_xMin = _points.get(0).x;
			_xMax = _points.get(1).x;
		}
		else
		{
			_xMin = _points.get(1).x;
			_xMax = _points.get(0).x;
		}
		if(_points.get(0).y < _points.get(1).y)
		{
			_yMin = _points.get(0).y;
			_yMax = _points.get(1).y;
		}
		else
		{
			_yMin = _points.get(1).y;
			_yMax = _points.get(0).y;
		}
		return this;
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
		GArray<Location> poly = new GArray<Location>(4);
		poly.add(new Location(_xMin, _yMin, _zMin, _zMax));
		poly.add(new Location(_xMin, _yMax, _zMin, _zMax));
		poly.add(new Location(_xMax, _yMax, _zMin, _zMax));
		poly.add(new Location(_xMin, _yMax, _zMin, _zMax));
		return poly;
	}
}
