package ru.l2gw.gameserver.model.entity.instance;

import ru.l2gw.util.Location;
import ru.l2gw.commons.math.Rnd;

import java.util.List;

/**
 * @author: rage
 * @date: 23.07.2009 18:49:40
 */
public class InstanceSpawn
{
	public final Location loc;
	public final String event;
	private final int npcId;
	public final int count;
	public final int delay;
	public final int respawn;
	public final int radius;
	public final int location;
	private final List<Integer> randomNpcList;

	public InstanceSpawn(String event, int npcId, Location loc, int count, int respawn, int delay, int radius, List<Integer> randomNpcList, int location)
	{
		this.event = event;
		this.npcId = npcId;
		this.loc = loc;
		this.count = count;
		this.respawn = respawn;
		this.delay = delay;
		this.radius = radius;
		this.randomNpcList = randomNpcList;
		this.location = location;
	}

	public int getNpcId()
	{
		if(randomNpcList != null && randomNpcList.size() > 0)
			return randomNpcList.get(Rnd.get(randomNpcList.size()));

		return npcId;
	}
}
