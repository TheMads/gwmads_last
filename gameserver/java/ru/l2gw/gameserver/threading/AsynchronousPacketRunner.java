package ru.l2gw.gameserver.threading;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.clientpackets.L2GameClientPacket;
import ru.l2gw.gameserver.network.GameClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AsynchronousPacketRunner implements PacketRunner
{

	private ExecutorService executor;

	public AsynchronousPacketRunner()
	{
		this.executor = new ThreadPoolExecutor(Config.GENERAL_PACKET_THREAD_CORE_SIZE, Config.GENERAL_PACKET_THREAD_CORE_SIZE + 2, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("Normal Packet Pool", DEFAULT_THREAD_PRIORITY));
	}

	public void runPacket(L2GameClientPacket packet)
	{
		if(!executor.isShutdown())
		{
			executor.execute(packet);
		}
	}

	public void removeContext(GameClient client)
	{}

	public void shutdown()
	{
		executor.shutdown();
	}
}
