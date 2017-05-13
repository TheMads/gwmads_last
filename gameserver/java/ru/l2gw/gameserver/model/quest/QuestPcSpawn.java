package ru.l2gw.gameserver.model.quest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

public class QuestPcSpawn
{
	public class DeSpawnScheduleTimerTask implements Runnable
	{
		int _ObjectId = 0;

		public DeSpawnScheduleTimerTask(int objectId)
		{
			_ObjectId = objectId;
		}

		public void run()
		{
			try
			{
				removeSpawn(_ObjectId);
			}
			catch(Throwable t)
			{}
		}
	}

	protected static Log _log = LogFactory.getLog(QuestPcSpawn.class.getName());

	private final int _playerObjectId;
	private GArray<L2Spawn> _spawns = new GArray<L2Spawn>();

	public QuestPcSpawn(L2Player player)
	{
		_playerObjectId = player.getObjectId();
	}

	private void addDeSpawnTask(int objectId, int spawnLength)
	{
		if(spawnLength > 0)
			ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnScheduleTimerTask(objectId), spawnLength);
	}

	/**
	 * Add spawn for player instance
	 * Return object id of newly spawned npc
	 */
	public int addSpawn(int npcId)
	{
		if(getPlayer() == null)
			return 0;
		Location _loc = getPlayer().getLoc();
		return addSpawn(npcId, _loc.getX(), _loc.getY(), _loc.getZ(), true);
	}

	/**
	 * Add spawn for player instance
	 * Will despawn after the spawn length expires
	 * Return object id of newly spawned npc
	 */
	public int addSpawn(int npcId, int spawnLength)
	{
		if(getPlayer() == null)
			return 0;
		Location _loc = getPlayer().getLoc();
		return addSpawn(npcId, _loc.getX(), _loc.getY(), _loc.getZ(), true, spawnLength);
	}

	/**
	 * Add spawn for player instance
	 * Return object id of newly spawned npc
	 */
	public int addSpawn(int npcId, int x, int y, int z)
	{
		return addSpawn(npcId, x, y, z, false);
	}

	/**
	 * Add spawn for player instance
	 * Return object id of newly spawned npc
	 */
	public int addSpawn(int npcId, int x, int y, int z, boolean randomOffset)
	{
		try
		{
			if(randomOffset)
			{
				// Get the direction of the offset
				x += Rnd.get(50, 100) * (Rnd.nextBoolean() ? 1 : -1);
				// Get the direction of the offset
				y += Rnd.get(50, 100) * (Rnd.nextBoolean() ? 1 : -1);
			}

			L2NpcTemplate template1 = NpcTable.getTemplate(npcId);
			if(template1 != null)
			{
				L2Spawn spawn = new L2Spawn(template1);

				spawn.setId(npcId);
				if(getPlayer() != null)
					spawn.setHeading(getPlayer().getHeading());
				else
					spawn.setHeading(0);
				spawn.setLocx(x);
				spawn.setLocy(y);
				spawn.setLocz(z + 20);
				spawn.stopRespawn();

				spawn.spawnOne();

				_spawns.add(spawn);

				return spawn.getId();
			}
		}
		catch(Exception e1)
		{
			_log.warn("Could not spawn Npc " + npcId);
		}

		return 0;
	}

	/**
	 * Add spawn for player instance
	 * Will despawn after the spawn length expires
	 * Return object id of newly spawned npc
	 */
	public int addSpawn(int npcId, int x, int y, int z, boolean randomOffset, int spawnLength)
	{
		int objectId = addSpawn(npcId, x, y, z, randomOffset);
		addDeSpawnTask(objectId, spawnLength);
		return objectId;
	}

	/**
	 * Add spawn for player instance
	 * Will despawn after the spawn length expires
	 * Return object id of newly spawned npc
	 */
	public int addSpawn(int npcId, int x, int y, int z, int spawnLength)
	{
		return addSpawn(npcId, x, y, z, false, spawnLength);
	}

	/** Return current player instance */
	public L2Player getPlayer()
	{
		return L2ObjectsStorage.getPlayer(_playerObjectId);
	}

	/**
	 * Return spawn instance for player instance
	 */
	public L2Spawn getSpawn(int objectId)
	{
		for(L2Spawn spawn : getSpawns())
			if(spawn.getId() == objectId)
				return spawn;
		return null;
	}

	/**
	 * Return list of L2Spawn for player instance
	 */
	public GArray<L2Spawn> getSpawns()
	{
		if(_spawns == null)
			_spawns = new GArray<L2Spawn>();
		return _spawns;
	}

	/**
	 * Return true if spawn was created by addSpawn method.
	 * @param npcId
	 * @return
	 */
	public boolean isSpawnExists(int npcId)
	{
		for(L2Spawn spawn : _spawns)
			if(spawn.getNpcId() == npcId)
				return true;
		return false;
	}

	public L2NpcInstance getSpawnedNpcById(int npcId)
	{
		for(L2Spawn spawn : _spawns)
			if(spawn.getNpcId() == npcId)
				return spawn.getLastSpawn();
		return null;
	}

	/**
	 * Remove all spawn for player instance
	 */
	public void removeAllSpawn()
	{
		if(_spawns == null)
			return;

		for(L2Spawn spawn : _spawns)
			spawn.despawnAll();

		_spawns.clear();
	}

	/**
	 * Remove spawn with object id for player instance
	 */
	public void removeSpawn(int objectId)
	{
		for(int i = 0; i < getSpawns().size(); i++)
			if(getSpawns().get(i).getId() == objectId)
			{
				L2NpcInstance npc = getSpawns().get(i).getLastSpawn();
				if(npc != null)
					npc.decayMe();
				getSpawns().remove(i);
				return;
			}
	}
}
