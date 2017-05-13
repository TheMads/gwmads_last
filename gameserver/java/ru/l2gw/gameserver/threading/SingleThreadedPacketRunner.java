package ru.l2gw.gameserver.threading;

import ru.l2gw.gameserver.clientpackets.L2GameClientPacket;
import ru.l2gw.gameserver.network.GameClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SingleThreadedPacketRunner implements PacketRunner
{

	private ExecutorService executor = Executors.newSingleThreadExecutor(new PriorityThreadFactory("Normal Packet Pool", DEFAULT_THREAD_PRIORITY));

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
