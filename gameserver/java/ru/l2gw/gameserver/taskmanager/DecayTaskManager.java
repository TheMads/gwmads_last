package ru.l2gw.gameserver.taskmanager;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;

import java.util.Map;

public class DecayTaskManager
{
	private static final Log log = LogFactory.getLog(DecayTaskManager.class.getName());
	private static DecayTaskManager _instance;
	private static Map<Long, Long> _lists;

	private DecayTaskManager()
	{
		_lists = new FastMap<Long, Long>().shared();
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new DecayScheduler(), 1000, 1000);
	}

	public static DecayTaskManager getInstance()
	{
		if(_instance == null)
			_instance = new DecayTaskManager();

		return _instance;
	}

	public void addDecayTask(L2Character actor)
	{
		_lists.put(actor.getStoredId(), System.currentTimeMillis() + actor.getTemplate().corpse_time * (actor.isMonster() && ((L2MonsterInstance) actor).isSweepActive() ? 2 : 1));
	}

	public void addDecayTask(L2Character actor, long interval)
	{
		long curTime = System.currentTimeMillis();
		long calculatedInterval = curTime + interval;
		if(calculatedInterval < curTime)
		{
			log.warn("DecayTaskManager: Added decay task interval is negative.");
			Thread.dumpStack();
		}

		_lists.put(actor.getStoredId(), calculatedInterval);
	}

	public void cancelDecayTask(L2Character actor)
	{
		_lists.remove(actor.getStoredId());
	}

	public long getDecayTime(L2Character actor)
	{
		Long dl = _lists.get(actor.getStoredId());
		if(dl != null)
			return dl;

		return 0;
	}

	public class DecayScheduler implements Runnable
	{
		public void run()
		{
			try
			{
				Long current = System.currentTimeMillis();
				for(Long actorStoredId : _lists.keySet())
				{
					Long dl = _lists.get(actorStoredId);
					if(dl != null)
					{
						if(current > dl)
						{
							L2Character actor = L2ObjectsStorage.getAsCharacter(actorStoredId);
							if(actor != null)
								actor.onDecay();
							_lists.remove(actorStoredId);
						}
					}
					else
						_lists.remove(actorStoredId);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("============= DecayTask Manager Report ============\n\r");
		sb.append("Tasks count: ").append(_lists.size()).append("\n\r");
		sb.append("Tasks dump:\n\r");

		long current = System.currentTimeMillis();
		for(Long actorStoredId : _lists.keySet())
		{
			L2Character actor = L2ObjectsStorage.getAsCharacter(actorStoredId);
			if(actor != null)
			{
				sb.append("Class/Name: ").append(actor.getClass().getSimpleName()).append('(').append(actor.getObjectId()).append(')').append('/').append(actor.getName());
				sb.append(" decay timer: ").append(_lists.get(actorStoredId) - current).append(" ms.\n\r");
			}
		}

		return sb.toString();
	}
}