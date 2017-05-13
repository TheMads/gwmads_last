package ru.l2gw.gameserver.instancemanager.superpoint;

import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 25.11.2010 13:57:46
 */
public class SuperpointNode extends Location
{
	private final int _fStringId;
	private final int _social;
	private final int _delay;
	private final int _id;
	
	public SuperpointNode(int x, int y, int z, int fStringId, int social, int delay, int id)
	{
		super(x, y, z);
		_fStringId = fStringId;
		_social = social;
		_delay = delay * 1000;
		_id = id;
	}

	public int getFStringId()
	{
		return _fStringId;
	}

	public int getSocial()
	{
		return _social;
	}

	public int getDelay()
	{
		return _delay;
	}

	public int getNodeId()
	{
		return _id;
	}
}
