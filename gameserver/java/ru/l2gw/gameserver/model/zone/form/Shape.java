package ru.l2gw.gameserver.model.zone.form;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

import java.awt.*;
import java.lang.reflect.Constructor;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 19.12.2008
 * Time: 15:23:33
 */
public abstract class Shape
{
	protected static Log _log = LogFactory.getLog(Shape.class.getName());

	protected GArray<Point> _points;
	protected int _zMin, _zMax, _zoneId;
	protected boolean _checkZ = false;
	protected boolean _exclude = false;

	public boolean isExclude()
	{
		return _exclude;
	}

	public int getMaxZ()
	{
		return _zMax;
	}

	public int getMinZ()
	{
		return _zMin;
	}

	public int getZoneId()
	{
		return _zoneId;
	}

	public boolean contains(L2Object obj)
	{
		return contains(obj.getX(), obj.getY(), obj.getZ());
	}

	public boolean contains(Location loc)
	{
		return contains(loc.getX(), loc.getY(), loc.getZ());
	}

	public boolean contains(int x, int y, int z)
	{
		return (!_checkZ || (z >= _zMin && z <= _zMax)) && contains(x, y);
	}

	public abstract boolean contains(int x, int y);

	public abstract boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2);

	public abstract double getDistanceToZone(int x, int y);

	protected abstract Shape prepare();

	protected static boolean lineSegmentsIntersect(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2)
	{
		return java.awt.geom.Line2D.linesIntersect(ax1, ay1, ax2, ay2, bx1, by1, bx2, by2);
	}

	public boolean checkZ()
	{
		return _checkZ;
	}

	public GArray<Point> getPoints()
	{
		return _points;
	}

	public static Shape parseShape(Node sn, int zoneId)
	{
		String type = "";
		Shape shape = null;
		Class<?> clazz;
		Constructor<?> constructor;
		try
		{
			type = sn.getAttributes().getNamedItem("type").getNodeValue();
			clazz = Class.forName("ru.l2gw.gameserver.model.zone.form.Shape" + type);
			constructor = clazz.getConstructor();
			shape = (Shape) constructor.newInstance();
		}
		catch(Exception e)
		{
			_log.error("Cannot create a Shape" + type + " zoneId " + zoneId + " " + e);
			e.printStackTrace();
			return null;
		}

		shape._points = new GArray<Point>();
		shape._zoneId = zoneId;
		for(Node n = sn.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if("point".equalsIgnoreCase(n.getNodeName()))
			{
				Point point;
				try
				{
					point = new Point(Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue()), Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue()));
				}
				catch(NullPointerException npe)
				{
					_log.error("x or y value missing in zone " + zoneId);
					return null;
				}
				catch(NumberFormatException nfe)
				{
					_log.error("x or y value not a number in zone " + zoneId);
					return null;
				}

				shape._points.add(point);
			}
		}

		if("Cylinder".equalsIgnoreCase(type))
		{
			try
			{
				int rad = Integer.parseInt(sn.getAttributes().getNamedItem("radius").getNodeValue());
				((ShapeCylinder) shape).setRadius(rad);
			}
			catch(Exception e)
			{
				_log.warn("missing or wrong radius for cylinder in zone " + zoneId);
				return null;
			}
		}

		Node z1 = sn.getAttributes().getNamedItem("zMin");
		Node z2 = sn.getAttributes().getNamedItem("zMax");
		if(z1 != null && z2 != null)
		{
			try
			{
				shape._zMin = Integer.parseInt(z1.getNodeValue());
				shape._zMax = Integer.parseInt(z2.getNodeValue());
				shape._checkZ = !(shape._zMin == shape._zMax);
			}
			catch(NumberFormatException nfe)
			{
				_log.error("zMin or zMax value not a number in zone " + zoneId);
				return null;
			}
		}

		Node ex = sn.getAttributes().getNamedItem("exclude");
		if(ex != null)
		{
			try
			{
				shape._exclude = Boolean.parseBoolean(ex.getNodeValue());
			}
			catch(Exception e)
			{
				_log.error("Invalid value for exclude in zone " + zoneId);
			}
		}

		Shape result = shape.prepare();
		if(result != null)
		{
			result._points.clear();
			result._points = null;
		}
		return result;
	}

	public abstract int getXMin();

	public abstract int getXMax();

	public abstract int getYMin();

	public abstract int getYMax();

	public abstract GArray<Location> getPolygon();
}
