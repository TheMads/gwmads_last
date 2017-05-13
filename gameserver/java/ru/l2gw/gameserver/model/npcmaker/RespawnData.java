package ru.l2gw.gameserver.model.npcmaker;

import ru.l2gw.util.Location;

/**
 * @author: rage
 * @date: 24.08.11 17:39
 */
public class RespawnData
{
	public final String dbname;
	public final long respawnTime;
	public final int currentHp;
	public final int currentMp;
	public final Location position;

	public RespawnData(String name, long respawn, int hp, int mp, int x, int y, int z)
	{
		dbname = name;
		respawnTime = respawn;
		currentHp = hp;
		currentMp = mp;
		position = new Location(x, y, z);
	}
}
