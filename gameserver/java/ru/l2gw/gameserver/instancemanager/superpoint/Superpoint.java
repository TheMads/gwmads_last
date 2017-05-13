package ru.l2gw.gameserver.instancemanager.superpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

/**
 * @author rage
 * @date 25.11.2010 14:34:18
 */
public class Superpoint
{
	private static final Log _log = LogFactory.getLog(Superpoint.class);

	private final String _name;
	private final int _type;
	private final GArray<SuperpointNode> _nodes;

	public Superpoint(String name, String type)
	{
		_name = name;
		_type = "rail".equalsIgnoreCase(type) ? 0 : 1;
		_nodes = new GArray<SuperpointNode>(2);
	}

	public void addNode(int x, int y, int z, int fStringId, int social, int delay, int id)
	{
		_nodes.add(new SuperpointNode(x, y, z, fStringId, social, delay, id));
	}

	public SuperpointNode getNextNode(L2Character cha, int moveType)
	{
		if(_nodes.size() < 2)
		{
			_log.warn(this + " nodes size < 2 for: " + cha);
			return null;
		}
		long minDist = Long.MAX_VALUE;
		int currentPoint = -1;
		for(int i = 0; i < _nodes.size(); i++)
		{
			SuperpointNode node = _nodes.get(i);
			double dist = cha.getDistance(node.getX(), node.getY(), node.getZ());
			if(dist < minDist)
			{
				minDist = (long) dist;
				currentPoint = i;
			}
		}

		if(_type == 0)
		{
			int np;
			if(moveType == 2)
			{
				np = currentPoint + 1;
				if(np >= _nodes.size())
					np = 0;
			}
			else
			{
				np = currentPoint + (cha.getAI().superPointDirection ? 1 : -1);

				if(np >= _nodes.size())
				{
					np = _nodes.size() - 2;
					cha.getAI().superPointDirection = false;
				}
				else if(np < 0)
				{
					np = 1;
					cha.getAI().superPointDirection = true;
				}
			}

			SuperpointNode currPoint = _nodes.get(currentPoint);
			SuperpointNode nextPoint = _nodes.get(np);
			while(cha.isInRange(nextPoint, 50))
			{
				if(cha.getAI().superPointDirection)
					np++;
				else
					np--;

				if(moveType == 2)
				{
					if(np >= _nodes.size())
						np = 0;
				}
				else
				{
					if(np >= _nodes.size())
					{
						np = _nodes.size() - 2;
						cha.getAI().superPointDirection = false;
					}
					else if(np < 0)
					{
						np = 1;
						cha.getAI().superPointDirection = true;
					}
				}

				nextPoint = _nodes.get(np);
			}

			if(cha.isInRangeZ(currPoint, 50))
				return nextPoint;

			return currPoint;
		}
		else if(_type == 1)
		{
			int c = 0;
			int np;
			while((np = Rnd.get(_nodes.size())) == currentPoint || c > 40)
				c++;

			if(c > 40)
				_log.warn(this + " can't make point for: " + cha);

			return _nodes.get(np);
		}

		return null;
	}

	public GArray<SuperpointNode> getNodes()
	{
		return _nodes;
	}

	public String getName()
	{
		return _name;
	}

	@Override
	public String toString()
	{
		return "Superpoint[name=" + _name + ";nodes=" + _nodes.size() + "]";
	}
}
