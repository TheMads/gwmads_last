package ru.l2gw.gameserver.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.clientpackets.L2GameClientPacket;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.threading.*;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.*;

/**
 * <p>This class is made to handle all the ThreadPools used in ru.l2gw</p>
 * <p>Scheduled Tasks can either be sent to a {@link #_generalScheduledThreadPool "general"} or {@link #_effectsScheduledThreadPool "effects"} {@link ScheduledThreadPoolExecutor ScheduledThreadPool}:
 * The "effects" one is used for every effects (skills, hp/mp regen ...) while the "general" one is used for
 * everything else that needs to be scheduled.<br>
 * There also is an {@link #_npcAiScheduledThreadPool "ai"} {@link ScheduledThreadPoolExecutor ScheduledThreadPool} used for AI Tasks.</p>
 * <p>Tasks can be sent to {@link ScheduledThreadPoolExecutor ScheduledThreadPool} either with:
 * <ul>
 * <li>{@link #scheduleEffect(Runnable,long)} : for effects Tasks that needs to be executed only once.</li>
 * <li>{@link #scheduleGeneral(Runnable,long)} : for scheduled Tasks that needs to be executed once.</li>
 * <li>{@link #scheduleAi(Runnable,long,boolean)} : for AI Tasks that needs to be executed once</li>
 * </ul>
 * or
 * <ul>
 * <li>{@link #scheduleEffectAtFixedRate(Runnable,long,long)(Runnable, long)} : for effects Tasks that needs to be executed periodicaly.</li>
 * <li>{@link #scheduleGeneralAtFixedRate(Runnable,long,long)(Runnable, long)} : for scheduled Tasks that needs to be executed periodicaly.</li>
 * <li>{@link #scheduleAiAtFixedRate(Runnable,long,long)(Runnable, long)} : for AI Tasks that needs to be executed periodicaly</li>
 * </ul></p>
 *
 * <p>For all Tasks that should be executed with no delay asynchronously in a ThreadPool there also are usual {@link ThreadPoolExecutor ThreadPools}
 * that can grow/shrink according to their load.:
 * <ul>
 * <li>There will be an AI ThreadPool where AI events should be executed</li>
 * </ul>
 * </p>
 * @author -Wooden-
 *
 */
@SuppressWarnings( { "unchecked" })
public class ThreadPoolManager
{
	private static final Log _log = LogFactory.getLog(ThreadPoolManager.class.getName());

	private static ThreadPoolManager _instance;

	private ScheduledThreadPoolExecutor _effectsScheduledThreadPool;
	private ScheduledThreadPoolExecutor _generalScheduledThreadPool;

	private ThreadPoolExecutor _LsGsExecutor;

	// temp
	private ScheduledThreadPoolExecutor _npcAiScheduledThreadPool;
	private ScheduledThreadPoolExecutor _playerAiScheduledThreadPool;
	private ScheduledThreadPoolExecutor _moveScheduledThreadPool;

	private boolean _shutdown;

	private final PacketRunner packetRunner;

	public static ThreadPoolManager getInstance()
	{
		if(_instance == null)
			_instance = new ThreadPoolManager();
		return _instance;
	}

	private ThreadPoolManager()
	{
		_moveScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_MOVE, new PriorityThreadFactory("MovePool", Thread.NORM_PRIORITY + 3));
		_effectsScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_EFFECTS, new PriorityThreadFactory("EffectsPool", Thread.MIN_PRIORITY));
		_generalScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_GENERAL, new PriorityThreadFactory("GeneralPool", Thread.NORM_PRIORITY));
		_npcAiScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.NPC_AI_MAX_THREAD, new PriorityThreadFactory("NpcAIPool", Thread.NORM_PRIORITY - 2));
		_playerAiScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.PLAYER_AI_MAX_THREAD, new PriorityThreadFactory("PlayerAIPool", Thread.NORM_PRIORITY + 1));

		_LsGsExecutor = new ThreadPoolExecutor(1, 6, 5L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("LS/GS Communications", Thread.MAX_PRIORITY - 2));

		if(Config.THREADING_MODEL == 1)
		{
			packetRunner = new SingleThreadedPacketRunner();
		}
		else if(Config.THREADING_MODEL == 2)
		{
			packetRunner = new AsynchronousPacketRunner();
		}
		else
		{ // Config.THREADING_MODEL == 3
			packetRunner = new ContextPacketRunner();
		}
	}

	public <T extends Runnable> ScheduledFuture<T> scheduleEffect(T r, long delay)
	{
		try
		{
			if(delay < 0)
				delay = 0;
			return (ScheduledFuture<T>) _effectsScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warn("ThreadPoolManager[99]: Failed executing new task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public <T extends Runnable> ScheduledFuture<T> scheduleEffectAtFixedRate(T r, long initial, long delay)
	{
		try
		{
			if(delay < 0)
				delay = 0;
			if(initial < 0)
				initial = 0;
			return (ScheduledFuture<T>) _effectsScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warn("ThreadPoolManager[120]: Failed executing new task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public <T extends Runnable> ScheduledFuture<T> scheduleGeneral(T r, long delay)
	{
		try
		{
			if(delay < 0)
				delay = 0;
			return (ScheduledFuture<T>) _generalScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warn("ThreadPoolManager[139]: Failed executing new task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public <T extends Runnable> ScheduledFuture<T> scheduleGeneralAtFixedRate(T r, long initial, long delay)
	{
		try
		{
			if(delay <= 0)
				delay = 1;
			return (ScheduledFuture<T>) _generalScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warn("ThreadPoolManager[158]: Failed executing new task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public <T extends Runnable> ScheduledFuture<T> scheduleAi(T r, long delay, boolean player)
	{
		try
		{
			if(delay < 0)
				delay = 0;
			if(player)
				return (ScheduledFuture<T>) _playerAiScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
			else
			//{
			//	Log.add("s:" + (r instanceof L2CharacterAI.Timer ? r : r.getClass().getSimpleName()), "npcthread");
				return (ScheduledFuture<T>) _npcAiScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
			//}
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warn("ThreadPoolManager[177]: Failed executing new task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public <T extends Runnable> ScheduledFuture<T> scheduleAiAtFixedRate(T r, long initial, long delay)
	{
		return scheduleAiAtFixedRate(r, initial, delay, false);
	}

	public <T extends Runnable> ScheduledFuture<T> scheduleAiAtFixedRate(T r, long initial, long delay, boolean player)
	{
		try
		{
			if(delay < 0)
				delay = 0;
			if(initial < 0)
				initial = 0;
			//if(!player)
			//{
			//	Log.add("r:" + r.getClass().getSimpleName(), "npcthread");
			//}
			return (ScheduledFuture<T>) (player ? _playerAiScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS) : _npcAiScheduledThreadPool.scheduleAtFixedRate(r, initial, delay, TimeUnit.MILLISECONDS));
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warn("ThreadPoolManager[198]: Failed executing new task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public void executeAi(Runnable r, boolean isPlayer)
	{
		try
		{
			if(isPlayer)
				_playerAiScheduledThreadPool.execute(r);
			else
			//{
				//Log.add("e:" + r.getClass().getSimpleName(), "npcthread");
				_npcAiScheduledThreadPool.execute(r);
			//}
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warn("MoveThreadPool: Failed schedule task!");
				Thread.dumpStack();
			}
		}
	}

	public ScheduledFuture<?> scheduleMove(Runnable r, long delay)
	{
		try
		{
			return _moveScheduledThreadPool.schedule(new RunnableWrapper(r), delay > 0 ? delay : 1, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warn("MoveThreadPool: Failed schedule task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public void executeMove(Runnable r)
	{
		_moveScheduledThreadPool.execute(r);
	}

	public void executeLSGSPacket(Runnable r)
	{
		_LsGsExecutor.execute(r);
	}

	public void executeGameClientPacket(L2GameClientPacket packet)
	{
		packetRunner.runPacket(packet);
	}

	public void onClientDisconnection(GameClient client)
	{
		packetRunner.removeContext(client);
	}

	public String[] getStats()
	{
		return new String[] {
				"Scheduled Thread Pools:",
				" + Effects:",
				" |- ActiveThreads:   " + _effectsScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _effectsScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _effectsScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _effectsScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _effectsScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_effectsScheduledThreadPool.getTaskCount() - _effectsScheduledThreadPool.getCompletedTaskCount()),
				" |- Shutdown:  " + _effectsScheduledThreadPool.isShutdown(),
				" | -------",
				" + General:",
				" |- ActiveThreads:   " + _generalScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _generalScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _generalScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _generalScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _generalScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_generalScheduledThreadPool.getTaskCount() - _generalScheduledThreadPool.getCompletedTaskCount()),
				" |- Shutdown:  " + _generalScheduledThreadPool.isShutdown(),
				" | -------",
				" + Move:",
				" |- ActiveThreads:   " + _moveScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _moveScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _moveScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _moveScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _moveScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_moveScheduledThreadPool.getTaskCount() - _moveScheduledThreadPool.getCompletedTaskCount()),
				" |- Shutdown:  " + _moveScheduledThreadPool.isShutdown(),
				" | -------",
				" + AI:",
				" |---- NPCs ----",
				" |- ActiveThreads:   " + _npcAiScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _npcAiScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _npcAiScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _npcAiScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _npcAiScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_npcAiScheduledThreadPool.getTaskCount() - _npcAiScheduledThreadPool.getCompletedTaskCount()),
				" |- Shutdown:  " + _npcAiScheduledThreadPool.isShutdown(),
				" |---- Players ----",
				" |- ActiveThreads:   " + _playerAiScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _playerAiScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _playerAiScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _playerAiScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _playerAiScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_playerAiScheduledThreadPool.getTaskCount() - _playerAiScheduledThreadPool.getCompletedTaskCount()),
				" |- Shutdown:  " + _playerAiScheduledThreadPool.isShutdown(),
				" | -------",
				"Thread Pools:",
				" + Packets:",
				" + LS/GS Packets:",
				" |- ActiveThreads:   " + _LsGsExecutor.getActiveCount(),
				" |- getCorePoolSize: " + _LsGsExecutor.getCorePoolSize(),
				" |- MaximumPoolSize: " + _LsGsExecutor.getMaximumPoolSize(),
				" |- LargestPoolSize: " + _LsGsExecutor.getLargestPoolSize(),
				" |- PoolSize:        " + _LsGsExecutor.getPoolSize(),
				" |- CompletedTasks:  " + _LsGsExecutor.getCompletedTaskCount(),
				" |- QueuedTasks:     " + _LsGsExecutor.getQueue().size(),
				" |- Shutdown:  " + _LsGsExecutor.isShutdown(),
				" | -------", };
	}

	/**
	*
	*/
	public void shutdown()
	{
		_shutdown = true;
		try
		{
			_effectsScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_npcAiScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_playerAiScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_moveScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_LsGsExecutor.awaitTermination(1, TimeUnit.SECONDS);
			_moveScheduledThreadPool.shutdown();
			_effectsScheduledThreadPool.shutdown();
			_generalScheduledThreadPool.shutdown();
			_npcAiScheduledThreadPool.shutdown();
			_playerAiScheduledThreadPool.shutdown();
			_LsGsExecutor.shutdown();
			packetRunner.shutdown();
			System.out.println("All ThreadPools are now stoped.");

		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Tries to remove from the work queue all {@link java.util.concurrent.Future}
	 * tasks that have been cancelled. This method can be useful as a
	 * storage reclamation operation, that has no other impact on
	 * functionality. Cancelled tasks are never executed, but may
	 * accumulate in work queues until worker threads can actively
	 * remove them. Invoking this method instead tries to remove them now.
	 * However, this method may fail to remove tasks in
	 * the presence of interference by other threads.
	 */
	public void purge()
	{
		_effectsScheduledThreadPool.purge();
		_generalScheduledThreadPool.purge();
		_npcAiScheduledThreadPool.purge();
		_playerAiScheduledThreadPool.purge();
		_LsGsExecutor.purge();
	}

	public String getGeneralPoolStats()
	{
		return getThreadPoolStats(_generalScheduledThreadPool, "general");
	}

	public String getAIPoolStats(boolean player)
	{
		if(player)
			return getThreadPoolStats(_playerAiScheduledThreadPool, "player ai");

		return getThreadPoolStats(_npcAiScheduledThreadPool, "npc ai");
	}

	private String getThreadPoolStats(ThreadPoolExecutor pool, String poolname)
	{
		ThreadFactory tf = pool.getThreadFactory();
		if(!(tf instanceof PriorityThreadFactory))
			return "This should not be seen, pool " + poolname;

		StringBuilder res = new StringBuilder();
		PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
		int count = ptf.getGroup().activeCount();
		Thread[] threads = new Thread[count + 2];
		ptf.getGroup().enumerate(threads);

		res.append("\nThread Pool: ").append(poolname);
		res.append("\nTasks in the queue: ").append(pool.getQueue().size());
		res.append("\nThreads stack trace:");
		res.append("\nThere should be ").append(count).append(" threads\n");

		for(Thread t : threads)
		{
			if(t == null)
				continue;
			res.append("\n").append(t.getName());
			for(StackTraceElement ste : t.getStackTrace())
				res.append("\n").append(ste.toString());
		}

		return res.toString();
	}

	public boolean isShutdown()
	{
		return _shutdown;
	}

	public long getNpcThreadCount()
	{
		return _npcAiScheduledThreadPool.getTaskCount() - _npcAiScheduledThreadPool.getCompletedTaskCount();
	}

	private static final class RunnableWrapper implements Runnable
	{
		private final Runnable _r;

		public RunnableWrapper(final Runnable r)
		{
			_r = r;
		}

		@Override
		public final void run()
		{
			try
			{
				_r.run();
			}
			catch (final Throwable e)
			{
				final Thread t = Thread.currentThread();
				final UncaughtExceptionHandler h = t.getUncaughtExceptionHandler();
				if (h != null)
					h.uncaughtException(t, e);
			}
		}
	}
}
