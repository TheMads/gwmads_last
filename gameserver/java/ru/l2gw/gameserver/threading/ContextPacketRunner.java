package ru.l2gw.gameserver.threading;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.clientpackets.L2GameClientPacket;
import ru.l2gw.gameserver.network.GameClient;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextPacketRunner implements PacketRunner
{

	private static final Log log = LogFactory.getLog(ContextPacketRunner.class.getName());

	private final AtomicBoolean shutdown = new AtomicBoolean(false);

	private final FastList<L2GameClientPacket> packets = new FastList<L2GameClientPacket>();

	private final GameClient[] activeClients = new GameClient[Config.GENERAL_PACKET_THREAD_CORE_SIZE];

	public ContextPacketRunner()
	{
		ThreadGroup threadGroup = new ThreadGroup("L2 ContextPacketRunner");
		for(int i = 0; i < Config.GENERAL_PACKET_THREAD_CORE_SIZE; i++)
		{
			Thread t = new Thread(threadGroup, new WorkingThread(), "L2 ContextPacketRunner Thread - " + i);
			t.setPriority(Thread.MAX_PRIORITY - 3);
			t.start();
		}
	}

	public void runPacket(L2GameClientPacket packet)
	{
		if(shutdown.get() || packet == null){ return; }

		GameClient client = packet.getClient();
		if(client == null || GameClient.GameClientState.DISCONNECTED.equals(client.getState())){ return; }

		synchronized (packets)
		{
			packets.addLast(packet);
		}
	}

	public void removeContext(GameClient client)
	{
		synchronized (packets)
		{
			Iterator<L2GameClientPacket> iterator = packets.iterator();

			while(iterator.hasNext())
			{
				L2GameClientPacket info = iterator.next();
				if(info.getClient().equals(client))
				{
					iterator.remove();
				}
			}
		}
	}

	public void shutdown()
	{
		shutdown.set(true);
	}

	private L2GameClientPacket getPacketForRun()
	{

		synchronized (packets)
		{
			Iterator<L2GameClientPacket> iterator = packets.iterator();
			outer: while(iterator.hasNext())
			{
				L2GameClientPacket packet = iterator.next();
				GameClient client = packet.getClient();
				synchronized (activeClients)
				{
					for(int i = 0, n = activeClients.length; i < n; i++)
					{
						if(activeClients[i] == client)
						{
							continue outer;
						}

						if(activeClients[i] == null)
						{
							activeClients[i] = client;
							iterator.remove();
							return packet;
						}
					}
				}
			}
		}

		return null;
	}

	private void unlockClient(GameClient client)
	{

		synchronized (activeClients)
		{
			for(int i = 0, n = activeClients.length; i < n; i++)
			{
				if(activeClients[i] == client)
				{
					activeClients[i] = null;
					return;
				}
			}
		}

		log.warn("Attempt to unlock not locked client.");
	}

	private class WorkingThread implements Runnable
	{
		public void run()
		{

			while(!shutdown.get())
			{

				L2GameClientPacket packet = getPacketForRun();

				if(packet != null)
				{
					try
					{
						packet.run();
					}
					catch(Exception e)
					{
						log.warn("Uncaught exception in L2GameClientPacket.runImpl()", e);
					}
					finally
					{
						unlockClient(packet.getClient());
					}

				}
				else
				{
					try
					{
						Thread.sleep(5);
					}
					catch(InterruptedException e)
					{
						log.warn("", e);
					}
				}
			}
		}
	}
}