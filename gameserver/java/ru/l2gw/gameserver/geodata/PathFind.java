package ru.l2gw.gameserver.geodata;

import ru.l2gw.util.Location;

import java.util.ArrayList;

/**
 * @Author: Diamond
 * @Date: 20/5/2007
 * @Time: 9:57:48
 */
public class PathFind
{
	private static final byte EAST = 1, WEST = 2, SOUTH = 4, NORTH = 8;

	private int mapSize;

	private GeoNode[][] map;
	private GeoNode[] mapFast;

	private int mapFastIndex = 0;
	private int mapFastSize = 0;

	private int offsetX;
	private int offsetY;

	private Location startpoint;
	private Location endpoint;

	private ArrayList<GeoNode> path;
	private int refIndex;

	long time;

	public PathFind(int x, int y, int z, int destX, int destY, int destZ, int refIndex)
	{
		time = System.currentTimeMillis();
		this.refIndex = refIndex;
		//startpoint = new Location(x - L2World.MAP_MIN_X >> 4, y - L2World.MAP_MIN_Y >> 4, z);
		//endpoint = new Location(destX - L2World.MAP_MIN_X >> 4, destY - L2World.MAP_MIN_Y >> 4, destZ);

		startpoint = GeoEngine.moveCheckWithCollision(x, y, z, destX, destY, true, refIndex).world2geo();
		endpoint = GeoEngine.moveCheckWithCollision(destX, destY, destZ, x, y, true, refIndex).world2geo();

		Layer[] layers1 = GeoEngine.NGetLayers(endpoint.getX(), endpoint.getY(), refIndex);

		short z1 = Short.MIN_VALUE;
		short NSWE1 = 15;

		for(Layer layer : layers1)
			if(Math.abs(endpoint.getZ() - z1) > Math.abs(endpoint.getZ() - layer.height))
			{
				z1 = layer.height;
				NSWE1 = layer.nswe;
			}

		if(NSWE1 == 0)
			endpoint = startpoint;

		int xdiff = Math.abs(endpoint.getX() - startpoint.getX());
		int ydiff = Math.abs(endpoint.getY() - startpoint.getY());

		// Please do not uncomment this if block, it makes possible to walk throught walls
		//		if(xdiff == 0 && ydiff == 0 || xdiff + ydiff == 1)
		//		{
		//			if(Math.abs(endpoint.getZ() - startpoint.getZ()) < 64)
		//			{
		//				path = new ArrayList<GeoNode>();
		//				path.add(0, new GeoNode(startpoint, 0, null));
		//			}
		//			return;
		//		}

		mapSize = 64 + Math.max(xdiff, ydiff);

		if(mapSize > 500)
			return;

		mapFastSize = mapSize * mapSize;

		map = new GeoNode[mapSize][mapSize];
		mapFast = new GeoNode[mapFastSize];

		offsetX = startpoint.getX() - mapSize / 2;
		offsetY = startpoint.getY() - mapSize / 2;

		path = findPath();
	}

	private GeoNode getBestOpenNode()
	{
		GeoNode bestNode = null;
		int bestNodeIndex = 0;

		for(int i = 0; i < mapFastIndex; i++)
		{
			GeoNode n = mapFast[i];
			if(n.closed)
				continue;
			if(bestNode == null || n.score < bestNode.score)
			{
				bestNode = n;
				bestNodeIndex = i;
			}
		}

		if(bestNode != null)
		{
			bestNode.closed = true;
			mapFastIndex--;
			mapFast[bestNodeIndex] = mapFast[mapFastIndex];
		}

		return bestNode;
	}

	private ArrayList<GeoNode> tracePath(GeoNode f)
	{
		ArrayList<GeoNode> nodes = new ArrayList<GeoNode>();
		GeoNode parent = f.parent;
		nodes.add(0, f);

		while(true)
		{
			if(parent.parent == null)
				break;
			nodes.add(0, parent);
			parent = parent.parent;
		}
		return nodes;
	}

	public ArrayList<GeoNode> findPath()
	{
		GeoNode n = new GeoNode(startpoint, 0, null);
		map[startpoint.getX() - offsetX][startpoint.getY() - offsetY] = n;
		mapFast[mapFastIndex] = n;
		mapFastIndex++;

		GeoNode nextNode;
		nextNode = getBestOpenNode();
		GeoNode finish;

		while(nextNode != null)
		{
			// Ограничение по числу пройденных точек, не более чем mapFastSize
			if(mapFastIndex >= mapFastSize)
				return null;

			if((finish = handleNode(nextNode)) != null)
				return tracePath(finish);

			// Ограничение по времени поиска
			if(System.currentTimeMillis() - time > 50)
				return null;

			nextNode = getBestOpenNode();
		}
		return null;
	}

	public GeoNode handleNode(GeoNode node)
	{
		Location cl = node.location;
		GeoNode result = null;

		int clX = cl.getX();
		int clY = cl.getY();
		int clZ = cl.getZ();
		short NSWE;

		NSWE = GeoEngine.NgetNSWE(clX, clY, clZ, refIndex);

		if((NSWE & EAST) == EAST)
			result = getNeighbour(clX + 1, clY, node);

		if(result != null)
			return result;

		if((NSWE & WEST) == WEST)
			result = getNeighbour(clX - 1, clY, node);

		if(result != null)
			return result;

		if((NSWE & SOUTH) == SOUTH)
			result = getNeighbour(clX, clY + 1, node);

		if(result != null)
			return result;

		if((NSWE & NORTH) == NORTH)
			result = getNeighbour(clX, clY - 1, node);

		return result;
	}

	public GeoNode getNeighbour(int x, int y, GeoNode from)
	{
		if(mapFastIndex >= mapFastSize)
			return null;

		if(x - offsetX > mapSize - 1 || x < offsetX || y - offsetY > mapSize - 1 || y < offsetY)
			return null;

		if(map[x - offsetX][y - offsetY] != null && map[x - offsetX][y - offsetY].closed)
			return null;

		int z = GeoEngine.NgetHeight(x, y, from.location.getZ(), refIndex);

		int height = Math.abs(z - from.location.getZ());

		if(height >= 32)
			return null;

		int weight = 0;

		if(GeoEngine.NgetNSWE(x, y, z, refIndex) != 15 || height > 8)
			weight = 8;
		else if(GeoEngine.NgetNSWE(x + 1, y, z, refIndex) != 15 || Math.abs(z - GeoEngine.NgetHeight(x + 1, y, z, refIndex)) > 8)
			weight = 4;
		else if(GeoEngine.NgetNSWE(x - 1, y, z, refIndex) != 15 || Math.abs(z - GeoEngine.NgetHeight(x - 1, y, z, refIndex)) > 8)
			weight = 4;
		else if(GeoEngine.NgetNSWE(x, y + 1, z, refIndex) != 15 || Math.abs(z - GeoEngine.NgetHeight(x, y + 1, z, refIndex)) > 8)
			weight = 4;
		else if(GeoEngine.NgetNSWE(x, y - 1, z, refIndex) != 15 || Math.abs(z - GeoEngine.NgetHeight(x, y - 1, z, refIndex)) > 8)
			weight = 4;

		int dx = Math.abs(endpoint.getX() - x);
		int dy = Math.abs(endpoint.getY() - y);
		int dz = Math.abs(endpoint.getZ() - z) / 16;

		GeoNode n = new GeoNode(new Location(x, y, z), 1, from);

		n.moveCost += from.moveCost + weight;
		n.score = n.moveCost + (int) Math.sqrt(dx * dx + dy * dy + dz * dz);

		if(x == endpoint.getX() && y == endpoint.getY() && Math.abs(z - endpoint.getZ()) < 64)
			return n;

		if(n.score < mapSize * 3)
		{
			GeoNode on = map[x - offsetX][y - offsetY];
			if(on == null || n.moveCost < on.moveCost)
			{
				map[x - offsetX][y - offsetY] = n;
				mapFast[mapFastIndex] = n;
				mapFastIndex++;
			}
		}

		return null;
	}

	public class GeoNode
	{
		public GeoNode parent = null;
		public Location location = null;
		public int moveCost = 0;
		public int score = 0;
		public boolean closed = false;

		public GeoNode(Location loc, int mCost, GeoNode pNode)
		{
			location = loc;
			moveCost = mCost;
			parent = pNode;
		}
	}

	public ArrayList<GeoNode> getPath()
	{
		return path;
	}
}
