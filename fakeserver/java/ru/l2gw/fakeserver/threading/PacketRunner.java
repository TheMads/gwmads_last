package ru.l2gw.fakeserver.threading;

import ru.l2gw.fakeserver.network.FakeClient;
import ru.l2gw.fakeserver.network.clientpackets.ClientPacket;

/**
 * @author: rage
 * @date: 18.04.13 13:28
 */
public interface PacketRunner
{
	static final int DEFAULT_THREAD_PRIORITY = Thread.MAX_PRIORITY - 3;

	public void runPacket(ClientPacket packet);

	public void removeContext(FakeClient client);

	public void shutdown();
}
