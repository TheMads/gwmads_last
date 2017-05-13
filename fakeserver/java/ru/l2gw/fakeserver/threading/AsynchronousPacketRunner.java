package ru.l2gw.fakeserver.threading;

import ru.l2gw.fakeserver.Config;
import ru.l2gw.fakeserver.network.FakeClient;
import ru.l2gw.fakeserver.network.clientpackets.ClientPacket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: rage
 * @date: 18.04.13 13:30
 */
public class AsynchronousPacketRunner implements PacketRunner
{

	private ExecutorService executor;

	public AsynchronousPacketRunner()
	{
		this.executor = new ThreadPoolExecutor(Config.GENERAL_PACKET_THREAD_CORE_SIZE, Config.GENERAL_PACKET_THREAD_CORE_SIZE + 2, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("Normal Packet Pool", DEFAULT_THREAD_PRIORITY));
	}

	public void runPacket(ClientPacket packet)
	{
		if(!executor.isShutdown())
		{
			executor.execute(packet);
		}
	}

	public void removeContext(FakeClient client)
	{}

	public void shutdown()
	{
		executor.shutdown();
	}
}
