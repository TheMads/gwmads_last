package ru.l2gw.gameserver.model.npcmaker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Territory;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.util.Location;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @author: rage
 * @date: 18.08.11 15:29
 */
public class DefaultMaker implements Cloneable
{
	protected static final Log _log = LogFactory.getLog(DefaultMaker.class);

	public final int maximum_npc;
	public final String name;
	public int on_start_spawn = 1;
	public int debug = 0;
	public volatile int npc_count;

	protected int i_ai0;
	protected int i_ai1;
	protected int i_ai2;
	protected int i_ai3;
	protected int i_ai4;
	protected int i_ai5;
	protected int i_ai6;
	protected int i_ai7;
	protected int i_ai8;

	protected final GArray<L2Territory> territories = new GArray<>(1);
	protected GArray<SpawnDefine> spawn_defines = new GArray<>(1);
	private Map<Integer, ScheduledFuture<?>> _timers;
	protected int reflectionId;

	public DefaultMaker(int maximum_npc, String name)
	{
		this.maximum_npc = maximum_npc;
		this.name = name;
	}

	public Location getRandomPos(boolean flying)
	{
		L2Territory terr = territories.get(Rnd.get(territories.size()));
		int[] p = terr.getRandomPoint(flying);
		if(p[0] == 0 && p[1] == 0 && p[2] == 0)
			return null;

		return new Location(p[0], p[1], p[2], Rnd.get(0xFFFF));
	}

	public void addTerritory(L2Territory terr)
	{
		territories.add(terr);
	}

	public void addBannedTerritory(L2Territory banned_terr)
	{
		for(L2Territory terr : territories)
			terr.addBannedTerritory(banned_terr);
	}

	public void addSpawnDefine(SpawnDefine sd)
	{
		spawn_defines.add(sd);
	}

	public void onStart()
	{
		if(on_start_spawn != 0)
			for(SpawnDefine sd : spawn_defines)
			{
				if(debug > 0)
					_log.info(this + " onStart: " + sd);
				if(maximum_npc >= npc_count + sd.total && atomicIncrease(sd, sd.total))
					sd.spawn(sd.total, 0, 0);
			}
	}

	public void onNpcDeleted(L2NpcInstance npc)
	{
		if(debug > 0)
			_log.info(this + " npc deleted: " + npc);
		SpawnDefine sd = npc.getSpawnDefine();
		if(sd.respawn != 0 && maximum_npc >= npc_count + 1 && atomicIncrease(sd, 1))
			sd.respawn(npc, sd.respawn, sd.respawn_rand);
		else
			npc.deleteMe();
	}

	public void onNpcCreated(L2NpcInstance npc)
	{
		if(debug > 0)
			_log.info(this + " npc created: " + npc);
	}

	public void onAllNpcDeleted()
	{
		if(debug > 0)
			_log.info(this + " all npc deleted.");
	}

	public void onScriptEvent(int eventId, Object arg1, Object arg2)
	{
		if(debug > 0)
			_log.info(this + " script event: " + eventId + " " + arg1 + " " + arg2);
		if(eventId == 1000)
			despawn();
		else if(eventId == 1001)
			for(SpawnDefine spawnDefine : spawn_defines)
			{
				int c = spawnDefine.total - spawnDefine.npc_count;
				if(c > 0 && maximum_npc >= npc_count + c && atomicIncrease(spawnDefine, c))
					spawnDefine.spawn(c, (Integer) arg1, 0);
			}
	}

	public final boolean isInside(int x, int y)
	{
		for(L2Territory territory : territories)
			if(territory.isInside(x, y))
				return true;

		return false;
	}

	public void onEvtTimer(int timerId, Object arg1, Object arg2)
	{}

	public final void despawn()
	{
		if(debug > 0)
			_log.info(this + " despawn()");
		for(SpawnDefine spawnDefine : spawn_defines)
			spawnDefine.despawn();

		npc_count = 0;
	}

	public final void save()
	{
		for(SpawnDefine spawnDefine : spawn_defines)
			spawnDefine.save();
	}

	public final void stopTimers()
	{
		if(_timers != null)
		{
			for(ScheduledFuture<?> task : _timers.values())
				task.cancel(true);

			_timers.clear();
		}
	}

	public final synchronized boolean atomicIncrease(SpawnDefine sd, int total)
	{
		if(maximum_npc >= npc_count + total && sd.total >= sd.npc_count + total)
		{
			npc_count += total;
			sd.npc_count += total;
			if(debug > 0)
				_log.info(this + " atomicIncrease: maximum_npc=" + maximum_npc + " npc_count=" + npc_count + " sd.total=" + sd.total + " sd.npc_count=" + sd.npc_count + " total=" + total + " " + sd + " true");
			return true;
		}

		if(debug > 0)
			_log.info(this + " atomicIncrease: maximum_npc=" + maximum_npc + " npc_count=" + npc_count + " sd.total=" + sd.total + " sd.npc_count=" + sd.npc_count + " total=" + total + " " + sd + " false");

		return false;
	}

	public final synchronized boolean atomicIncrease(int total)
	{
		if(maximum_npc >= npc_count + total)
		{
			npc_count += total;
			if(debug > 0)
				_log.info(this + " atomicIncrease: maximum_npc=" + maximum_npc + " npc_count=" + npc_count + " true");
			return true;
		}

		if(debug > 0)
			_log.info(this + " atomicIncrease: maximum_npc=" + maximum_npc + " npc_count=" + npc_count + " false");

		return false;
	}

	public final synchronized boolean atomicDecrease(SpawnDefine sd, int total)
	{
		if(npc_count >= total && sd.npc_count >= total)
		{
			npc_count -= total;
			sd.npc_count -= total;
			if(debug > 0)
				_log.info(this + " atomicDecrease: npc_count=" + npc_count + " sd.npc_count=" + sd.npc_count + " total=" + total + " true");
			return true;
		}

		if(debug > 0)
			_log.info(this + " atomicDecrease: npc_count=" + npc_count + " sd.npc_count=" + sd.npc_count + " total=" + total + " false");
		return false;
	}

	public final synchronized boolean atomicDecrease(int total)
	{
		if(npc_count >= total)
		{
			npc_count -= total;
			if(debug > 0)
				_log.info(this + " atomicDecrease: npc_count=" + npc_count + " true");
			return true;
		}

		if(debug > 0)
			_log.info(this + " atomicDecrease: npc_count=" + npc_count + " false");

		return false;
	}

	public final void addTimer(int timerId, long delay)
	{
		addTimer(timerId, null, null, delay);
	}

	public final void addTimer(int timerId, Object arg1, long delay)
	{
		addTimer(timerId, arg1, null, delay);
	}

	public final void addTimer(int timerId, Object arg1, Object arg2, long delay)
	{
		if(_timers == null)
			_timers = new ConcurrentHashMap<>();

		ScheduledFuture<?> timer = ThreadPoolManager.getInstance().scheduleAi(new Timer(timerId, arg1, arg2), delay, false);
		if(timer != null)
			_timers.put(timerId, timer);
	}

	public final void blockTimer(int timerId)
	{
		if(_timers == null)
			return;

		ScheduledFuture<?> timer = _timers.remove(timerId);
		if(timer != null)
			timer.cancel(true);
	}

	protected class Timer implements Runnable
	{
		private int _timerId;
		private Object _arg1;
		private Object _arg2;

		public Timer(int timerId, Object arg1, Object arg2)
		{
			_timerId = timerId;
			_arg1 = arg1;
			_arg2 = arg2;
		}

		public void run()
		{
			if(_timers != null)
				_timers.remove(_timerId);
			onEvtTimer(_timerId, _arg1, _arg2);
		}
	}

	public void notifyScriptEvent(final int eventId, final Object arg1, final Object arg2)
	{
		ThreadPoolManager.getInstance().executeAi(new Runnable()
		{
			@Override
			public void run()
			{
				onScriptEvent(eventId, arg1, arg2);
			}
		}, false);
	}

	public boolean inTerritory(int x, int y)
	{
		for(L2Territory terr : territories)
			if(terr.isInside(x, y))
				return true;

		return false;
	}

	public GArray<L2Territory> getTerritories()
	{
		return territories;
	}

	@Override
	public DefaultMaker clone()
	{
		try
		{
			DefaultMaker dm = (DefaultMaker) super.clone();
			dm.spawn_defines = new GArray<>(spawn_defines.size());
			if(debug > 0)
			    _log.info("Cloned maker: " + dm);
			for(SpawnDefine sd : spawn_defines)
			{
				SpawnDefine s = sd.clone();
				s.setMaker(dm);
				dm.spawn_defines.add(s);
			}
			return dm;
		}
		catch(CloneNotSupportedException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public void onInstanceZoneEvent(Instance inst, int eventId)
	{}

	public void setReflectionId(int refId)
	{
		reflectionId = refId;
		for(SpawnDefine sd : spawn_defines)
		{
			sd.setReflection(reflectionId);
		}
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + name + ";npc_count=" + npc_count + ";maximum_npc=" + maximum_npc + "]";
	}
}
