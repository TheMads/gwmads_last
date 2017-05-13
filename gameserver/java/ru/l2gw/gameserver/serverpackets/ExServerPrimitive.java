package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.superpoint.Superpoint;
import ru.l2gw.gameserver.instancemanager.superpoint.SuperpointNode;
import ru.l2gw.gameserver.model.L2Territory;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.model.zone.form.Shape;
import ru.l2gw.util.Location;

/**
 * @author: rage
 * @date: 09.12.11 16:35
 */
public class ExServerPrimitive extends L2GameServerPacket
{
	private final int x,y,z;
	private final String name;
	private final GArray<PointInfo> pointInfo;

	public ExServerPrimitive(L2Zone zone, int color_r, int color_g, int color_b)
	{
		name = zone.getZoneName();
		x = (zone.getMaxX() + zone.getMinX()) / 2;
		y = (zone.getMaxY() + zone.getMaxY()) / 2;
		z = (zone.getMaxZ() + zone.getMinZ()) / 2;
		pointInfo = new GArray<>();
		Shape[] shapes = zone.getShapes();
		if(shapes != null)
		{
			int s = 0;
			for(Shape shape : shapes)
			{
				GArray<Location> poly = shape.getPolygon();
				for(int i = 0; i < poly.size(); i++)
				{
					int p2 = i + 1 < poly.size() ? i + 1 : 0;
					PointInfo pi = new PointInfo();
					pi.name = name + "(" + i + "->" + p2 + ")[" + s + "]";
					pi.r = color_r;
					pi.g = color_g;
					pi.b = color_b;
					pi.points = new GArray<>(2);
					pi.points.add(poly.get(i).clone());
					pi.points.add(poly.get(p2).clone());
					pointInfo.add(pi);

					pi = new PointInfo();
					pi.name = name + "(" + i + "->" + p2 + ")[" + s + "]";
					pi.r = color_r;
					pi.g = color_g;
					pi.b = color_b;
					pi.points = new GArray<>(2);
					pi.points.add(poly.get(i).clone().setZ(poly.get(i).getHeading()));
					pi.points.add(poly.get(p2).clone().setZ(poly.get(p2).getHeading()));
					pointInfo.add(pi);

					pi = new PointInfo();
					pi.r = color_r;
					pi.g = color_g;
					pi.b = color_b;
					pi.name = "";
					pi.points = new GArray<>(2);
					pi.points.add(poly.get(i).clone());
					pi.points.add(poly.get(i).clone().setZ(poly.get(i).getHeading()));
					pointInfo.add(pi);
				}
				s++;
			}
		}

		shapes = zone.getExShapes();
		if(shapes != null)
		{
			int s = 0;
			for(Shape shape : shapes)
			{
				GArray<Location> poly = shape.getPolygon();
				for(int i = 0; i < poly.size(); i++)
				{
					int p2 = i + 1 < poly.size() ? i + 1 : 0;
					PointInfo pi = new PointInfo();
					pi.name = "(EX) " + name + "(" + i + "->" + p2 + ")[" + s + "]";
					pi.r = 0xFF;
					pi.points = new GArray<>(2);
					pi.points.add(poly.get(i).clone());
					pi.points.add(poly.get(p2).clone());
					pointInfo.add(pi);

					pi = new PointInfo();
					pi.name = "(EX) " + name + "(" + i + "->" + p2 + ")[" + s + "]";
					pi.r = 0xFF;
					pi.points = new GArray<>(2);
					pi.points.add(poly.get(i).clone().setZ(poly.get(i).getHeading()));
					pi.points.add(poly.get(p2).clone().setZ(poly.get(p2).getHeading()));
					pointInfo.add(pi);

					pi = new PointInfo();
					pi.r = 0xFF;
					pi.name = "";
					pi.points = new GArray<>(2);
					pi.points.add(poly.get(i).clone());
					pi.points.add(poly.get(i).clone().setZ(poly.get(i).getHeading()));
					pointInfo.add(pi);
				}
				s++;
			}
		}
	}

	public ExServerPrimitive(Superpoint sp, int color_r, int color_g, int color_b)
	{
		name = sp.getName();
		GArray<SuperpointNode> nodes = sp.getNodes();
		pointInfo = new GArray<>(nodes.size() * 2);
		if(nodes.size() > 0)
		{
			x = nodes.get(0).getX();
			y = nodes.get(0).getY();
			z = nodes.get(0).getZ();
			int n = 0;
			for(SuperpointNode sn : nodes)
			{
				PointInfo pi = new PointInfo();
				pi.r = color_r;
				pi.g = color_g;
				pi.b = color_b;
				pi.name = name + "[" + n + "]";
				pi.points = new GArray<>(1);
				pi.points.add(sn.clone());
				pointInfo.add(pi);
				n++;
			}

			for(int i = 0; i < nodes.size() - 1; i++)
			{
				PointInfo pi = new PointInfo();
				pi.r = color_r;
				pi.g = color_g;
				pi.b = color_b;
				pi.t = 0x80;
				pi.name = name + "(" + i + "->" + (i + 1) + ")";
				pi.points = new GArray<>(2);
				pi.points.add(nodes.get(i).clone());
				pi.points.add(nodes.get(i + 1).clone());
				pointInfo.add(pi);
			}
		}
		else
			x = y = z = 0;
	}

	public ExServerPrimitive(L2Territory terr, int color_r, int color_g, int color_b)
	{
		name = terr.getName();
		x = (terr.getXmax() + terr.getXmin()) / 2;
		y = (terr.getYmax() + terr.getYmin()) / 2;
		z = (terr.getZmax() + terr.getZmin()) / 2;
		pointInfo = new GArray<>();
		L2Territory.Point[] points = terr.getPoints();
		if(points.length > 0)
		{
			for(int i = 0; i < points.length; i++)
			{
				int p2 = i + 1 < points.length ? i + 1 : 0;
				PointInfo pi = new PointInfo();
				pi.name = name + "(" + i + "->" + p2 + ")";
				pi.r = color_r;
				pi.g = color_g;
				pi.b = color_b;
				pi.points = new GArray<>(2);
				pi.points.add(new Location(points[i].x, points[i].y, points[i].zmin));
				pi.points.add(new Location(points[p2].x, points[p2].y, points[p2].zmin));
				pointInfo.add(pi);

				pi = new PointInfo();
				pi.name = name + "(" + i + "->" + p2 + ")";
				pi.r = color_r;
				pi.g = color_g;
				pi.b = color_b;
				pi.points = new GArray<>(2);
				pi.points.add(new Location(points[i].x, points[i].y, points[i].zmax));
				pi.points.add(new Location(points[p2].x, points[p2].y, points[p2].zmax));
				pointInfo.add(pi);

				pi = new PointInfo();
				pi.r = color_r;
				pi.g = color_g;
				pi.b = color_b;
				pi.name = "";
				pi.points = new GArray<>(2);
				pi.points.add(new Location(points[i].x, points[i].y, points[i].zmin));
				pi.points.add(new Location(points[i].x, points[i].y, points[i].zmax));
				pointInfo.add(pi);
			}
		}
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x11);
		writeS(name);
		writeD(x);
		writeD(y);
		writeD(z);
		writeD(0x800); // ?
		writeD(0x800); // ?
		writeD(pointInfo.size());
		for(PointInfo pi : pointInfo)
		{
			writeC(pi.points.size());
			writeS(pi.name);
			writeD(pi.r);
			writeD(pi.g);
			writeD(pi.b);
			writeD(pi.t);
			for(Location point : pi.points)
			{
				writeD(point.getX());
				writeD(point.getY());
				writeD(point.getZ());
			}
		}
	}

	private static class PointInfo
	{
		public String name;
		public GArray<Location> points;
		public int r, g, b, t = 0xFF;
	}
}