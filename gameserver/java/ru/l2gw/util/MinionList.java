package ru.l2gw.util;

import javolution.util.FastMap;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.scripts.Script;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2MinionData;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.npcmaker.SpawnDefine;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

/**
 * This class ...
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */

public class MinionList
{
	private static org.apache.commons.logging.Log _log = LogFactory.getLog(L2MonsterInstance.class.getName());

	/**
	 * List containing the current spawned minions for this L2MonsterInstance
	 */
	private final ConcurrentLinkedQueue<L2NpcInstance> minionReferences;
	private final L2NpcInstance master;
	private FastMap<Integer, ScheduledFuture<?>> respawnTasks;

	public MinionList(L2NpcInstance master)
	{
		minionReferences = new ConcurrentLinkedQueue<>();
		this.master = master;
	}

	public int countSpawnedMinions()
	{
		synchronized(minionReferences)
		{
			return minionReferences.size();
		}
	}

	public byte countSpawnedMinionsById(L2MinionData minionData)
	{
		byte count = 0;
		synchronized(minionReferences)
		{
			for(L2NpcInstance minion : getSpawnedMinions())
				if(minion.getNpcId() == minionData.minionId && minion.getMinionData() == minionData)
					count++;
		}
		return count;
	}

	public boolean hasMinions()
	{
		return getSpawnedMinions().size() > 0;
	}

	public L2NpcInstance getMaster()
	{
		return master;
	}

	public ConcurrentLinkedQueue<L2NpcInstance> getSpawnedMinions()
	{
		return minionReferences;
	}

	public void addSpawnedMinion(L2NpcInstance minion)
	{
		if(!minionReferences.contains(minion))
			synchronized(minionReferences)
			{
				minionReferences.add(minion);
			}
	}

	public int lazyCountSpawnedMinionsGroups()
	{
		Set<Integer> seenGroups = new HashSet<Integer>();
		for(L2NpcInstance minion : getSpawnedMinions())
			seenGroups.add(minion.getNpcId());
		return seenGroups.size();
	}

	public void removeSpawnedMinion(L2NpcInstance minion)
	{
		synchronized(minionReferences)
		{
			if(minionReferences.contains(minion))
			{
				minionReferences.remove(minion);
			}
		}
	}

	public void notifyDead(L2NpcInstance npc)
	{
		synchronized(minionReferences)
		{
			if(npc != master)
				master.notifyAiEvent(master, CtrlEvent.EVT_PARTY_DIED, npc, null, null);
			for(L2NpcInstance party : minionReferences)
				if(!party.isDead() && party.isVisible())
					master.notifyAiEvent(party, CtrlEvent.EVT_PARTY_DIED, npc, null, null);
		}
	}

	public void respawnPrivate(L2NpcInstance minion, long respawn)
	{
		if(respawnTasks == null)
			respawnTasks = new FastMap<Integer, ScheduledFuture<?>>(minionReferences.size() + 1).shared();
		respawnTasks.put(minion.getObjectId(), ThreadPoolManager.getInstance().scheduleAi(new MinionRespawn(minion), respawn * 1000L, false));
	}

	/**
	 * Manage the spawn of all Minions of this RaidBoss.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the Minion data of all Minions that must be spawn </li>
	 * <li>For each Minion type, spawn the amount of Minion needed </li><BR><BR>
	 */
	public void maintainMinions()
	{
		GArray<L2MinionData> minions = master.getMinionsData();

		synchronized(minionReferences)
		{
			int minionsToSpawn;
			for(L2MinionData minion : minions)
			{
				minionsToSpawn = minion.minionAmount - countSpawnedMinionsById(minion);

				for(int i = 0; i < minionsToSpawn; i++)
					spawnSingleMinion(minion, null);
			}
		}
	}

	/**
	 * Manage the lonely Minions of this RaidBoss.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the Minion data of all Minions that still alive </li>
	 * <li>For each Minion type, unspawn the Minion </li><BR><BR>
	 */
	public void maintainLonelyMinions()
	{
		synchronized(minionReferences)
		{
			for(L2NpcInstance minion : getSpawnedMinions())
				if(!minion.isDead())
				{
					removeSpawnedMinion(minion);
					minion.deleteMe();
				}
			if(respawnTasks != null)
			{
				for(ScheduledFuture<?> task : respawnTasks.values())
					task.cancel(true);

				respawnTasks.clear();
			}
		}
	}

	/**
	 * Init a Minion and add it in the world as a visible object.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the template of the Minion to spawn </li>
	 * <li>Create and Init the Minion and generate its Identifier </li>
	 * <li>Set the Minion HP, MP and Heading </li>
	 * <li>Set the Minion leader to this RaidBoss </li>
	 * <li>Init the position of the Minion and add it in the world as a visible object </li><BR><BR>
	 *
	 * @param minionid The I2NpcTemplate Identifier of the Minion to spawn
	 */
	public L2NpcInstance spawnSingleMinion(L2MinionData minion, Location pos)
	{
		return spawnSingleMinion(minion, pos, 0, 0, 0);
	}

	public L2NpcInstance spawnSingleMinion(L2MinionData minion, Location pos, long p1, long p2, long p3)
	{
		SpawnDefine sd = master.getSpawnDefine();
		if(sd != null && sd.getMaker().npc_count + 1 > sd.getMaker().maximum_npc)
			return null;

		// Get the template of the Minion to spawn
		L2NpcTemplate minionTemplate = NpcTable.getTemplate(minion.minionId);
		if(minionTemplate == null)
		{
			_log.warn("No NPC template for id: " + minion.minionId);
			return null;
		}

		Constructor<?> _constructor;

		// Create the generic constructor of L2NpcInstance managed by this L2Spawn
		try
		{
			_constructor = Class.forName("ru.l2gw.gameserver.model.instances." + minionTemplate.type + "Instance").getConstructors()[0];
		}
		catch(ClassNotFoundException e)
		{
			Script script = Scripts.getInstance().getClasses().get("npc.model." + minionTemplate.type + "Instance");
			if(script == null)
			{
				_log.warn("Spawn minion: Script npc.model." + minionTemplate.type + "Instance.java not found or loaded with errors npc id: " + minion.minionId);
				return null;
			}
			_constructor = script.getRawClass().getConstructors()[0];
		}

		if(_constructor == null)
		{
			_log.warn("Spawn minion: npc.model." + minionTemplate.type + "Instance.java not found or loaded with errors npc id: " + minion.minionId);
			return null;
		}

		Object tmp;

		if(sd == null || sd.getMaker().atomicIncrease(1))
		{
			try
			{
				tmp = _constructor.newInstance(IdFactory.getInstance().getNextId(), minionTemplate, master.getStoredId(), p1, p2, p3);
			}
			catch(Exception e)
			{
				_log.warn("Spawn minion: can't create instance npc.model." + minionTemplate.type + "Instance.java npc id: " + minion.minionId);
				e.printStackTrace();
				return null;
			}

			if(!(tmp instanceof L2NpcInstance))
			{
				_log.warn("Spawn minion: " + minionTemplate.type + " not a L2Minion! spawn as L2Minion npc id: " + minion.minionId);
				tmp = new L2NpcInstance(IdFactory.getInstance().getNextId(), minionTemplate, master.getStoredId(), p1, p2, p3);
			}
			// Create and Init the Minion and generate its Identifier
			L2NpcInstance monster = (L2NpcInstance) tmp;

			if(minion.minionAi != null)
				monster.setAIConstructor(minion.minionAi);

			monster.setMinionData(minion);
			monster.weight_point = minion.weight_point;

			// Set the Minion HP, MP and Heading
			monster.setCurrentHpMp(monster.getMaxHp(), monster.getMaxMp());
			monster.setHeading(pos == null ? master.getHeading() : pos.getHeading());

			// Set master reflection
			monster.setReflection(master.getReflection());

			// Get position for new Minion
			if(pos == null)
				pos = getMinionPosition();

			// Init the position of the Minion and add it in the world as a visible object
			monster.spawnMe(pos);
			monster.setSpawnedLoc(pos);
			monster.onSpawn();

			if(Config.DEBUG)
				_log.info("Spawned minion template " + minionTemplate.npcId + " with objId: " + monster.getObjectId() + " to boss " + master.getObjectId() + " ,at: " + monster.getX() + " x, " + monster.getY() + " y, " + monster.getZ() + " z");
			return monster;
		}

		return null;
	}

	public void spawnSingleMinion(int minionId, String ai, int respawn, Location pos)
	{
		synchronized(minionReferences)
		{
			spawnSingleMinion(new L2MinionData(minionId, ai, 1, respawn, 0), pos);
		}
	}

	public Location getMinionPosition()
	{
		Location pos;
		int x, y, randomAngle, validRadius;

		int radiusBoss = (int) master.getColRadius();

		randomAngle = Rnd.get(360);
		validRadius = Rnd.get(5 * radiusBoss);

		x = (int) (master.getX() + (2 * radiusBoss + validRadius) * Math.cos(randomAngle));
		y = (int) (master.getY() + (2 * radiusBoss + validRadius) * Math.sin(randomAngle));

		pos = new Location(x, y, master.getZ());
		return pos;
	}

	private class MinionRespawn implements Runnable
	{
		L2NpcInstance minion;

		public MinionRespawn(L2NpcInstance mob)
		{
			minion = mob;
		}

		public void run()
		{
			L2NpcInstance master = minion.getLeader();
			respawnTasks.remove(minion.getObjectId());
			if(master != null && !master.isDead() && master.isVisible())
			{
				if(master.getSpawnDefine() != null && !master.getSpawnDefine().getMaker().atomicIncrease(1))
					return;

				minion.refreshID();
				minion.setCurrentHpMp(minion.getMaxHp(), minion.getMaxMp());
				minion.stopAllEffects();
				Location loc = getMinionPosition();
				minion.setReflection(master.getReflection());
				minion.setSpawnedLoc(loc);
				minion.spawnMe(loc);
				minion.onSpawn();
			}
		}
	}
}
