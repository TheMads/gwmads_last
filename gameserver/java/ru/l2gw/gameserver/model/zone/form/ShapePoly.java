package ru.l2gw.gameserver.model.zone.form;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

import java.awt.*;

public class ShapePoly extends Shape
{
	private int[] _x;
	private int[] _y;

	private int _xMin = Integer.MAX_VALUE, _xMax = Integer.MIN_VALUE;
	private int _yMin = _xMin, _yMax = _xMax;

	@Override
	public boolean contains(int x, int y)
	{
		boolean inside = false;
		for (int i = 0, j = _x.length-1; i < _x.length; j = i++)
		{
			if ( (((_y[i] <= y) && (y < _y[j])) || ((_y[j] <= y) && (y < _y[i]))) && (x < (_x[j] - _x[i]) * (y - _y[i]) / (_y[j] - _y[i]) + _x[i]) )
			{
				inside = !inside;
			}
		}
		return inside;
	}

	@Override
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		int tX, tY, uX, uY;
		// First check if a point of the polygon lies inside the rectangle
		if(_x[0] > ax1 && _x[0] < ax2 && _y[0] > ay1 && _y[0] < ay2) return true;

		// Or a point of the rectangle inside the polygon
		if(contains(ax1, ay1)) return true;

		// If the first point wasn't inside the rectangle it might still have any line crossing any side
		// of the rectangle

		// Check every possible line of the polygon for a collision with any of the rectangles side
		for(int i = 0; i < _y.length; i++)
		{
			tX = _x[i];
			tY = _y[i];
			uX = _x[(i + 1) % _x.length];
			uY = _y[(i + 1) % _x.length];

			// Check if this line intersects any of the four sites of the rectangle
			if(lineSegmentsIntersect(tX, tY, uX, uY, ax1, ay1, ax1, ay2)) return true;
			if(lineSegmentsIntersect(tX, tY, uX, uY, ax1, ay1, ax2, ay1)) return true;
			if(lineSegmentsIntersect(tX, tY, uX, uY, ax2, ay2, ax1, ay2)) return true;
			if(lineSegmentsIntersect(tX, tY, uX, uY, ax2, ay2, ax2, ay1)) return true;
		}

		return false;
	}

	@Override
	public double getDistanceToZone(int x, int y)
	{
		double test, shortestDist = Math.pow(_x[0] - x, 2) + Math.pow(_y[0] - y, 2);

		for(int i = 1; i < _y.length; i++)
		{
			test = Math.pow(_x[i] - x, 2) + Math.pow(_y[i] - y, 2);
			if(test < shortestDist) shortestDist = test;
		}

		return Math.sqrt(shortestDist);
	}

	@Override
	protected Shape prepare()
	{
		if(_points.size() < 3)
		{
			_log.error("Invalid point amount in shape " + _zoneId + ", must be >2");
			return null;
		}

		int size = _points.size();
		Point p;
		_x = new int[size];
		_y = new int[size];
		for(int i = 0; i < size; i++)
		{
			p = _points.get(i);
			_xMin = Math.min(_xMin, p.x);
			_xMax = Math.max(_xMax, p.x);
			_x[i] = p.x;
			_yMin = Math.min(_yMin, p.y);
			_yMax = Math.max(_yMax, p.y);
			_y[i] = p.y;
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
		GArray<Location> poly = new GArray<>(_x.length);
		for(int i = 0; i < _x.length; i++)
			poly.add(new Location(_x[i], _y[i], _zMin, _zMax));

		return poly;
	}
}
