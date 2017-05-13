/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.l2gw.util;

import org.apache.commons.logging.LogFactory;

import java.lang.management.*;

/**
 * Thanks to L2M :)
 *
 * @author -Nemesiss-
 */
public class DeadLockDetector implements Runnable
{

	private static final org.apache.commons.logging.Log log = LogFactory.getLog(DeadLockDetector.class.getName());

	private static DeadLockDetector instance;

	//	private final ListenerEngine<DeadLockDetector> listenerEngine = ListenerEngineFactory.newListenerEngine(this);

	private final ThreadMXBean tmx;

	public static DeadLockDetector getInstance()
	{
		return instance;
	}

	private DeadLockDetector()
	{
		tmx = ManagementFactory.getThreadMXBean();
		instance = this;
	}

	@Override
	public final void run()
	{

		boolean deadlock = false;
		while(!deadlock)
		{
			try
			{
				long[] ids = tmx.findDeadlockedThreads();

				// Deadlock detected
				if(ids != null)
				{

					deadlock = true;
					ThreadInfo[] tis = tmx.getThreadInfo(ids, true, true);
					String info = "DeadLock Found!\n";
					for(ThreadInfo ti : tis)
					{
						info += ti.toString();
					}

					for(ThreadInfo ti : tis)
					{
						LockInfo[] locks = ti.getLockedSynchronizers();
						MonitorInfo[] monitors = ti.getLockedMonitors();
						if(locks.length == 0 && monitors.length == 0)
						{
							// This thread isn't a reason, it's just blocked by external deadlock
							continue;
						}

						ThreadInfo dl = ti;
						info += "Java-level deadlock:\n";
						info += "\t" + dl.getThreadName() + " is waiting to lock " + dl.getLockInfo().toString() + " which is held by " + dl.getLockOwnerName() + "\n";
						while((dl = tmx.getThreadInfo(new long[] { dl.getLockOwnerId() }, true, true)[0]).getThreadId() != ti.getThreadId())
						{
							info += "\t" + dl.getThreadName() + " is waiting to lock " + dl.getLockInfo().toString() + " which is held by " + dl.getLockOwnerName() + "\n";
						}
					}

					//					listenerEngine.methodInvoked(DeadlockListener.class);

					log.warn(info);
					log.warn("Shutting down server with exit code = 1, startup script will do authomatic restart.");
					//					System.exit(1);
				}

				Thread.sleep(200);
			}
			catch(Exception e)
			{
				log.warn(e.getLocalizedMessage());
			}
		}
	}

	public static void start()
	{
		Thread t = new Thread(new DeadLockDetector());
		t.setName("DeadLock Monitor");
		t.setDaemon(true);
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
		log.info("DeadLock Detector started.");
	}
}
