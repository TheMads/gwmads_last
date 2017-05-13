package ru.l2gw.extensions.ccpGuard.packets;

import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;

/**
 * @author rage
 * @date 16.10.2010 11:50:23
 */
public class ProtectPing extends L2GameServerPacket
{
	private static int y;
	static
	{
		y = ConfigProtect.PROTECT_TITLE_Y;
		if(!ConfigProtect.PROTECT_SERVER_TITLE.isEmpty())
			y += 14;
		if(ConfigProtect.PROTECT_ONLINE_PACKET_TIME > 0)
			y += 14;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xB0); // Packet ID
		writeC(0xA2);
		writeC(0x01);
		writeD(ConfigProtect.PROTECT_TITLE_X);
		writeD(y);
		writeD(ConfigProtect.PROTECT_PING_COLOR);
		writeD(getClient().getPingTime());
	}
}
