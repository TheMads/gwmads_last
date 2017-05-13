package ru.l2gw.fakeserver.threading;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.fakeserver.network.FakeClient;
import ru.l2gw.fakeserver.network.clientpackets.ClientPacket;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: rage
 * @date: 18.04.13 13:27
 */
public class ThreadPoolManager
{
	private static final Log _log = LogFactory.getLog(ThreadPoolManager.class.getName());
	private static ThreadPoolManager _instance;
	private final PacketRunner packetRunner;
	private ScheduledThreadPoolExecutor generalScheduledThreadPool;

	public static ThreadPoolManager getInstance()
	{
		if(_instance == null)
			_instance = new ThreadPoolManager();
		return _instance;
	}

	private ThreadPoolManager()
	{
		packetRunner = new AsynchronousPacketRunner();
		generalScheduledThreadPool = new ScheduledThreadPoolExecutor(3, new PriorityThreadFactory("GeneralPool", Thread.NORM_PRIORITY));
	}

	public void executeGameClientPacket(ClientPacket packet)
	{
		packetRunner.runPacket(packet);
	}

	public void onClientDisconnection(FakeClient client)
	{
		packetRunner.removeContext(client);
	}

	public <T extends Runnable> ScheduledFuture<T> scheduleGeneralAtFixedRate(T r, long initial, long delay)
	{
		try
		{
			if(delay <= 0)
				delay = 1;
			return (ScheduledFuture<T>) generalScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			return null; /* shutdown, ignore */
		}
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
				final Thread.UncaughtExceptionHandler h = t.getUncaughtExceptionHandler();
				if (h != null)
					h.uncaughtException(t, e);
			}
		}
	}
}
