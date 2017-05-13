package ru.l2gw.gameserver.model.npcmaker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.scripts.Script;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2MinionData;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.tables.SpawnTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author: rage
 * @date: 18.08.11 14:52
 */
public class SpawnDefine implements Cloneable
{
	private static final Log _log = LogFactory.getLog(SpawnDefine.class);

	public final L2NpcTemplate npc_template;
	public HashMap<Integer, Location> positions;
	public final int total;
	public final int respawn;
	public final int respawn_rand;
	public int chase_pc = 0;
	public volatile int npc_count;

	private String dbname, dbsaving;
	private Constructor<?> constructor;
	private Constructor<?> ai_constructor = null;
	private StatsSet ai_params = null;
	private HashSet<L2NpcInstance> npc_list;
	private DefaultMaker maker;
	private GArray<L2MinionData> privates;
	private int reflectionId;
	private boolean bossSpawnSet;

	public SpawnDefine(L2NpcTemplate npc_template, int total, int respawn, int respawn_rand, String ai, String ai_params, String privateStr, String dbn, String dbs, DefaultMaker maker, boolean bossSet)
	{
		this.npc_template = npc_template;
		this.total = total;
		this.respawn = respawn;
		this.respawn_rand = respawn_rand;
		this.maker = maker;
		npc_list = new HashSet<>(total);
		dbname = dbn;
		dbsaving = dbs;
		bossSpawnSet = bossSet;

		try
		{
			constructor = Class.forName("ru.l2gw.gameserver.model.instances." + npc_template.type + "Instance").getConstructors()[0];
		}
		catch(ClassNotFoundException e)
		{
			Script script = Scripts.getInstance().getClasses().get("npc.model." + npc_template.type + "Instance");
			if(script == null)
				_log.warn(this + " npc type " + npc_template.type + " not found!");
			else
				constructor = script.getRawClass().getConstructors()[0];
		}

		if(constructor == null)
			_log.warn(this + " npc type " + npc_template.type + " not found!");

		if(ai != null && !ai.isEmpty())
			try
			{
				if(!ai.equalsIgnoreCase("npc"))
					ai_constructor = Class.forName("ru.l2gw.gameserver.ai." + ai).getConstructors()[0];
			}
			catch(Exception e)
			{
				try
				{
					ai_constructor = Scripts.getInstance().getClasses().get("ai." + ai).getRawClass().getConstructors()[0];
				}
				catch(Exception e1)
				{
					_log.warn(this + " AI type " + ai + " not found!");
				}
			}

		if(ai_params != null && !ai_params.isEmpty())
			for(String param : ai_params.split(";"))
				if(!param.isEmpty())
				{
					if(this.ai_params == null)
						this.ai_params = new StatsSet();
					this.ai_params.set(param.split("=")[0], param.split("=")[1]);
				}

		if(privateStr != null && !privateStr.isEmpty())
			for(String priv : privateStr.split(";"))
				if(priv != null && !priv.isEmpty())
				{
					String[] privateParams = priv.split(":");
					if(privateParams.length >= 4)
					{
						if(privates == null)
							privates = new GArray<>(1);

						privates.add(new L2MinionData(Integer.parseInt(privateParams[0]), privateParams[1], 1, SpawnTable.getSecFromString(privateParams[3]), Integer.parseInt(privateParams[2])));
					}
				}
	}

	public void respawn(L2NpcInstance npc, int respawn, int respawn_rand)
	{
		if(respawn == 0)
		{
			npc.refreshID();
			spawnNpc(npc, null);
		}
		else
		{
			long respawnDelay = respawn * 1000L + Rnd.get(-respawn_rand, respawn_rand) * 1000L;
			if(dbname != null && (dbsaving.contains("death_time") || bossSpawnSet))
			{
				_log.info(this + " Schedule respawn: " + new Date(System.currentTimeMillis() + respawnDelay));
				SpawnTable.getInstance().saveRespawn(dbname, System.currentTimeMillis() + respawnDelay, 0, 0, npc.getLoc());
			}
			RespawnManager.addRespawnNpc(npc, respawnDelay);
		}
	}

	public void spawn(int count, int respawn, int respawn_rand)
	{
		for(int i = 0; i < count; i++)
			if(constructor != null)
				try
				{
					L2NpcInstance npc = getFreeNpc();

					if(npc == null)
					{
						Object tmp = constructor.newInstance(IdFactory.getInstance().getNextId(), npc_template, 0L, 0L, 0L, 0L);

						// Check if the Instance is a L2NpcInstance
						if(!(tmp instanceof L2NpcInstance))
							continue;

						npc = (L2NpcInstance) tmp;
					}

					if(ai_constructor != null)
						npc.setAIConstructor(ai_constructor);

					if(ai_params != null)
						npc.setAIParams(ai_params);

					if(chase_pc > 0)
					{
						if(npc.getAIParams() == null)
						{
							StatsSet set = new StatsSet();
							npc.setAIParams(set);
						}

						if(npc.getAIParams().getInteger("MaxPursueRange", 0) == 0)
							npc.getAIParams().set("MaxPursueRange", chase_pc);
					}

					if(privates != null)
						npc.setMinionsData(privates);

					npc.setSpawnDefine(this);

					npc_list.add(npc);

					if(respawn == 0)
					{
						RespawnData rd;
						if(maker.debug > 0)
							_log.info(this + " respawn = 0 dbname=" + dbname);
						if(dbname != null && (rd = SpawnTable.getInstance().getRespawnData(dbname)) != null)
						{
							if(maker.debug > 0)
								_log.info(this + " respawnTime=" + rd.respawnTime + " currTime=" + System.currentTimeMillis());
							SpawnTable.getInstance().removeRespawnData(rd);
							if(rd.respawnTime > System.currentTimeMillis())
							{
								if(maker.debug > 0)
									_log.info(this + " add respawn delay: " + (rd.respawnTime - System.currentTimeMillis()));
								RespawnManager.addRespawnNpc(npc, rd.respawnTime - System.currentTimeMillis());
							}
							else
								spawnNpc(npc, rd);
						}
						else
							spawnNpc(npc, null);
					}
					else
						RespawnManager.addRespawnNpc(npc, respawn * 1000L + Rnd.get(-respawn_rand, respawn_rand) * 1000L);
				}
				catch(Exception e)
				{
					_log.warn(this + " " + maker + " can't spawn " + e.getMessage());
					e.printStackTrace();
				}
	}

	public DefaultMaker getMaker()
	{
		return maker;
	}

	public void setMaker(DefaultMaker mk)
	{
		maker = mk;
	}

	public void despawn()
	{
		for(L2NpcInstance npc : npc_list)
		{
			if(npc.isVisible())
				npc.deleteMe();
			else
				RespawnManager.cancelRespawn(npc);
		}
		maker.atomicDecrease(npc_count);
		npc_count = 0;
	}

	public void addPosition(Location pos, int chance)
	{
		if(positions == null)
			positions = new HashMap<>(1);

		positions.put(chance, pos);
	}

	public Location getRandomPosInMyTerritory(L2NpcInstance npc)
	{
		return positions == null ? maker.getRandomPos(npc.isFlying()) : getRandomPos();
	}

	public void spawnNpc(L2NpcInstance npc, RespawnData rd)
	{
		Location loc;
		if(rd != null && (dbsaving.contains("pos") || bossSpawnSet))
			loc = rd.position;
		else
			loc = getRandomPosInMyTerritory(npc);

		if(loc == null)
			return;

		// Set the HP and MP of the L2NpcInstance to the max
		if(rd != null && (dbsaving.contains("parameters") || bossSpawnSet))
			npc.setCurrentHpMp(rd.currentHp, rd.currentHp);
		else
			npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());

		npc.stopAllEffects();
		npc.setNpcState(0);
		npc.removeStatsOwner(npc);
		npc.setWeaponEnchant(0);
		// Link the L2NpcInstance to this L2Spawn
		npc.setSpawnDefine(this);

		// Set the heading of the L2NpcInstance (random heading if not defined)
		npc.setHeading(loc.getHeading());

		// save npc_list points
		npc.setSpawnedLoc(loc);

		if(reflectionId != 0)
			npc.setReflection(reflectionId);

		npc.spawnMe(loc);
		npc.onSpawn();
		if(maker.debug > 0)
			_log.info(this + " spawn: " + npc + " " + loc + " npc_count=" + npc_count);
	}

	private Location getRandomPos()
	{
		int chance = Rnd.get(100);
		for(Map.Entry<Integer, Location> entry : positions.entrySet())
			if(chance < entry.getKey())
				return entry.getValue().clone();
			else
				chance -= entry.getKey();

		return null;
	}

	private L2NpcInstance getFreeNpc()
	{
		for(L2NpcInstance npc : npc_list)
			if(!npc.isVisible() && !RespawnManager.contains(npc))
				return npc;

		return null;
	}

	public void sendScriptEvent(int eventId, Object arg1, Object arg2)
	{
		for(L2NpcInstance npc : npc_list)
			if(npc.isVisible())
				npc.getAI().sendScriptEvent(npc, eventId, arg1, arg2);
	}

	public void save()
	{
		if(dbname != null && (dbsaving.contains("parameters") || dbsaving.contains("pos") || dbsaving.contains("death_time") || bossSpawnSet))
			for(L2NpcInstance npc : npc_list)
				if(npc.isVisible() && npc.getMaxHp() != (int) npc.getCurrentHp())
					SpawnTable.getInstance().saveRespawn(dbname, npc.isDead() && (dbsaving.contains("death_time") || bossSpawnSet) ? respawn * 1000L + Rnd.get(-respawn_rand, respawn_rand) * 1000L : 0, (int) npc.getCurrentHp(), (int) npc.getCurrentMp(), npc.getLoc());
	}

	public void setReflection(int refId)
	{
		reflectionId = refId;
	}

	public void setChasePc(int chasePc)
	{
		chase_pc = chasePc;
	}

	@Override
	public SpawnDefine clone()
	{
		try
		{
			SpawnDefine sd = (SpawnDefine) super.clone();
			sd.npc_list = new HashSet<>();
			sd.maker = null;
			return sd;
		}
		catch(CloneNotSupportedException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String toString()
	{
		return "SpawnDefine[" + npc_template.name + ";id=" + npc_template.npcId + ";hash=" + hashCode() + "]";
	}
}
