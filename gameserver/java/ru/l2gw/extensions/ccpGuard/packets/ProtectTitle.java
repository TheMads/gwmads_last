package ru.l2gw.extensions.ccpGuard.packets;

import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;

/**
 * @author rage
 * @date 16.10.2010 0:26:07
 */
public class ProtectTitle extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0xB0); // Packet ID
		writeC(0xA0);
		writeC(0x01);
		writeD(ConfigProtect.PROTECT_TITLE_X);
		writeD(ConfigProtect.PROTECT_TITLE_Y);
		writeD(ConfigProtect.PROTECT_TITLE_COLOR);
		writeS(ConfigProtect.PROTECT_SERVER_TITLE);
	}
}
