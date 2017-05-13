package ru.l2gw.gameserver.model.npcmaker;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @author: rage
 * @date: 18.08.11 22:26
 */
public class RespawnManager
{
	private static final Map<Long, Long> respawn_list = new ConcurrentHashMap<>();
	private static ScheduledFuture<?> mainTask;

	public static void addRespawnNpc(L2NpcInstance npc, long delay)
	{
		if(mainTask == null)
			mainTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new RespawnTask(), 1000, 1000);

		respawn_list.put(npc.getStoredId(), System.currentTimeMillis() + delay);
	}

	public static void cancelRespawn(L2NpcInstance npc)
	{
		respawn_list.remove(npc.getStoredId());
	}

	public static boolean contains(L2NpcInstance npc)
	{
		return respawn_list.containsKey(npc.getStoredId());
	}

	public static class RespawnTask implements Runnable
	{
		public void run()
		{
			try
			{
				long current = System.currentTimeMillis();
				for(Long storedId : respawn_list.keySet())
				{
					L2NpcInstance npc = L2ObjectsStorage.getAsNpc(storedId);
					if(npc != null)
					{
						Long dl = respawn_list.get(storedId);
						SpawnDefine sd = npc.getSpawnDefine();
						if(dl != null && sd != null)
						{
							if(current > dl)
							{
								npc.refreshID();
								sd.spawnNpc(npc, null);
								respawn_list.remove(storedId);
							}
						}
						else
							respawn_list.remove(storedId);
					}
					else
						respawn_list.remove(storedId);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
