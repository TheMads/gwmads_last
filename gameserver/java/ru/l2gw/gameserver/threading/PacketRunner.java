package ru.l2gw.gameserver.threading;

import ru.l2gw.gameserver.clientpackets.L2GameClientPacket;
import ru.l2gw.gameserver.network.GameClient;

public interface PacketRunner
{

	static final int DEFAULT_THREAD_PRIORITY = Thread.MAX_PRIORITY - 3;

	public void runPacket(L2GameClientPacket packet);

	public void removeContext(GameClient client);

	public void shutdown();
}
