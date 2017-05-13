package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.GameServer;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.tables.FakePlayersTable;

public class SendStatus extends L2GameServerPacket
{
	private int _opcode;

	public SendStatus(int opcode)
	{
		_opcode = opcode;
	}

	@Override
	protected final void writeImpl()
	{
		_log.info("STATUS: Status request recieved from " + getClient().toString());

		writeC(0x2E); // Packet ID
		writeD(GameServer.getServerId()); // World ID
		writeD(Config.MAXIMUM_ONLINE_USERS); // Max Online
		writeD(L2ObjectsStorage.getAllPlayersCount() + FakePlayersTable.getFakePlayersCount() + (Rnd.chance(50) ? 2 : 0)); // Current Online
		writeD(L2ObjectsStorage.getAllPlayersCount() + FakePlayersTable.getFakePlayersCount()); // Current Online
		writeD(Config.SEND_STATUS_REAL_STORE ? L2ObjectsStorage.getAllOfflineCount() : (int) (L2ObjectsStorage.getAllPlayersCount() * 0.12)); // Priv.Sotre Chars

		// SEND TRASH
		writeD(0x002C0030);
		for (int x = 0; x < 10; x++)
			writeH(41 + Rnd.get(17));
		writeD(43 + Rnd.get(17));
		int z = 36219 + Rnd.get(1987);
		writeD(z);
		writeD(z);
		writeD(37211 + Rnd.get(2397));
		writeD(0x00);
		writeD(0x02);
	}
}
