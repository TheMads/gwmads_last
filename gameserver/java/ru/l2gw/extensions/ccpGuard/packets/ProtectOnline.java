package ru.l2gw.extensions.ccpGuard.packets;

import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.tables.FakePlayersTable;

/**
 * @author rage
 * @date 16.10.2010 0:34:57
 */
public class ProtectOnline extends L2GameServerPacket
{
	private static final int y = ConfigProtect.PROTECT_SERVER_TITLE.isEmpty() ? ConfigProtect.PROTECT_TITLE_Y : ConfigProtect.PROTECT_TITLE_Y + 14;

	@Override
	protected final void writeImpl()
	{
		writeC(0xB0); // Packet ID
		writeC(0xA1);
		writeC(0x01);
		writeD(ConfigProtect.PROTECT_TITLE_X);
		writeD(y);
		writeD(ConfigProtect.PROTECT_ONLINE_COLOR);
		writeD(L2ObjectsStorage.getAllPlayersCount() + FakePlayersTable.getFakePlayersCount());
	}
}
